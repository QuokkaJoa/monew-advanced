package com.part2.monew.repository;

import com.part2.monew.dto.response.CursorPageResponse;
import com.part2.monew.dto.response.InterestDto;
import com.part2.monew.entity.Interest;
import com.part2.monew.entity.QInterest;
import com.part2.monew.entity.QInterestKeyword;
import com.part2.monew.entity.QKeyword;
import com.part2.monew.entity.QUserSubscriber;
import com.part2.monew.mapper.InterestMapper;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class InterestRepositoryCustomImpl implements InterestRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final InterestMapper interestMapper;

  private static final QInterest interest = QInterest.interest;
  private static final QKeyword keyword = QKeyword.keyword;
  private static final QInterestKeyword interestKeyword = QInterestKeyword.interestKeyword;
  private static final QUserSubscriber userSubscriber = QUserSubscriber.userSubscriber;

  public InterestRepositoryCustomImpl(EntityManager em, InterestMapper interestMapper) {
    this.queryFactory = new JPAQueryFactory(em);
    this.interestMapper = interestMapper;
  }

  @Override
  public CursorPageResponse<InterestDto> searchInterestsWithQueryDsl(
      String keywordSearchTerm, String orderByField, String direction,
      String primaryCursorValue, String secondaryCursorValue, int limit, UUID requestUserId) {

    BooleanExpression predicate = buildWhereClause(keywordSearchTerm, orderByField, direction, primaryCursorValue, secondaryCursorValue);

    List<OrderSpecifier<?>> orderSpecifiers = buildOrderByClause(orderByField, direction);

    List<Tuple> results = queryFactory
        .select(
            interest,
            JPAExpressions.selectOne()
                .from(userSubscriber)
                .where(userSubscriber.user.id.eq(requestUserId)
                    .and(userSubscriber.interest.id.eq(interest.id)))
                .exists()
        )
        .from(interest)
        .leftJoin(interest.interestKeywords, interestKeyword).fetchJoin()
        .leftJoin(interestKeyword.keyword, keyword).fetchJoin()
        .where(predicate)
        .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
        .limit(limit + 1)
        .distinct()
        .fetch();

    boolean hasNext = results.size() > limit;
    List<Tuple> contentTuples = hasNext ? results.subList(0, limit) : results;

    List<InterestDto> interestDtos = contentTuples.stream()
        .map(tuple -> {
          Interest fetchedInterest = tuple.get(interest);
          Boolean isSubscribed = tuple.get(1, Boolean.class);
          return interestMapper.toDto(fetchedInterest, Boolean.TRUE.equals(isSubscribed));
        })
        .collect(Collectors.toList());

    String nextPrimaryCursor = null;
    String nextSecondaryCursor = null;
    if (hasNext && !contentTuples.isEmpty()) {
      Interest lastInterestInContent = contentTuples.get(contentTuples.size() - 1).get(interest);
      if (lastInterestInContent != null) {
        if ("name".equalsIgnoreCase(orderByField)) {
          nextPrimaryCursor = lastInterestInContent.getName();
        } else {
          nextPrimaryCursor = lastInterestInContent.getSubscriberCount().toString();
        }
        if (lastInterestInContent.getCreatedAt() != null) {
          nextSecondaryCursor = lastInterestInContent.getCreatedAt().toInstant().toString();
        }
      }
    }

    BooleanExpression countPredicate = buildWhereClause(keywordSearchTerm, null, null, null, null);
    JPAQuery<Long> countBaseQuery = queryFactory.select(interest.countDistinct()).from(interest);
    if (keywordSearchTerm != null && !keywordSearchTerm.isEmpty()) {
      countBaseQuery.leftJoin(interest.interestKeywords, interestKeyword)
          .leftJoin(interestKeyword.keyword, keyword);
    }
    Long totalElements = countBaseQuery.where(countPredicate).fetchOne();
    totalElements = (totalElements == null) ? 0L : totalElements;

    return CursorPageResponse.of(
        interestDtos,
        nextPrimaryCursor,
        nextSecondaryCursor,
        totalElements,
        hasNext
    );
  }

  private BooleanExpression buildWhereClause(String keywordSearchTerm, String orderByField, String direction,
      String primaryCursorValue, String secondaryCursorValue) {
    BooleanExpression predicate = Expressions.asBoolean(true).isTrue();
    if (keywordSearchTerm != null && !keywordSearchTerm.isEmpty()) {
      predicate = predicate.and(
          interest.name.containsIgnoreCase(keywordSearchTerm)
              .or(interest.interestKeywords.any().keyword.name.containsIgnoreCase(keywordSearchTerm))
      );
    }

    if (orderByField != null && primaryCursorValue != null && !primaryCursorValue.isEmpty()) {
      Timestamp createdAtCursorValue = null;
      if (secondaryCursorValue != null && !secondaryCursorValue.isEmpty()) {
        try {
          createdAtCursorValue = Timestamp.from(Instant.parse(secondaryCursorValue));
        } catch (Exception e) {
          log.warn("커서 파싱 오류 (secondaryCursorValue): {}", secondaryCursorValue, e);
        }
      }

      boolean isAsc = "ASC".equalsIgnoreCase(direction);

      if ("name".equalsIgnoreCase(orderByField)) {
        if (isAsc) {
          BooleanExpression primaryCondition = interest.name.gt(primaryCursorValue);
          if (createdAtCursorValue != null) {
            predicate = predicate.and(primaryCondition.or(interest.name.eq(primaryCursorValue).and(interest.createdAt.gt(createdAtCursorValue))));
          } else {
            predicate = predicate.and(primaryCondition);
          }
        } else {
          BooleanExpression primaryCondition = interest.name.lt(primaryCursorValue);
          if (createdAtCursorValue != null) {
            predicate = predicate.and(primaryCondition.or(interest.name.eq(primaryCursorValue).and(interest.createdAt.lt(createdAtCursorValue))));
          } else {
            predicate = predicate.and(primaryCondition);
          }
        }
      } else if ("subscriberCount".equalsIgnoreCase(orderByField)) {
        try {
          Integer countCursor = Integer.parseInt(primaryCursorValue);
          if (isAsc) {
            BooleanExpression primaryCondition = interest.subscriberCount.gt(countCursor);
            if (createdAtCursorValue != null) {
              predicate = predicate.and(primaryCondition.or(interest.subscriberCount.eq(countCursor).and(interest.createdAt.gt(createdAtCursorValue))));
            } else {
              predicate = predicate.and(primaryCondition);
            }
          } else {
            BooleanExpression primaryCondition = interest.subscriberCount.lt(countCursor);
            if (createdAtCursorValue != null) {
              predicate = predicate.and(primaryCondition.or(interest.subscriberCount.eq(countCursor).and(interest.createdAt.lt(createdAtCursorValue))));
            } else {
              predicate = predicate.and(primaryCondition);
            }
          }
        } catch (NumberFormatException e) {
          log.warn("구독자 수 커서 파싱 오류: {}", primaryCursorValue, e);
        }
      }
    }
    return predicate;
  }

  private List<OrderSpecifier<?>> buildOrderByClause(String orderByField, String direction) {
    List<OrderSpecifier<?>> orders = new ArrayList<>();
    boolean isAsc = "ASC".equalsIgnoreCase(direction);
    Order orderDirection = isAsc ? Order.ASC : Order.DESC;

    if ("name".equalsIgnoreCase(orderByField)) {
      orders.add(new OrderSpecifier<>(orderDirection, interest.name));
    } else if ("subscriberCount".equalsIgnoreCase(orderByField)) {
      orders.add(new OrderSpecifier<>(orderDirection, interest.subscriberCount));
    } else {
      log.warn("알 수 없는 정렬 기준 '{}', 기본 정렬(최신순)을 사용합니다.", orderByField);
      orders.add(new OrderSpecifier<>(Order.DESC, interest.createdAt));
    }


    if (!("createdAt".equalsIgnoreCase(orderByField))) {
      orders.add(new OrderSpecifier<>(orderDirection, interest.createdAt));
    }
    orders.add(new OrderSpecifier<>(orderDirection, interest.id));

    return orders;
  }
}
