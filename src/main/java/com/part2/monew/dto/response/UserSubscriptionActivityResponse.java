package com.part2.monew.dto.response;

import com.part2.monew.entity.Interest;
import com.part2.monew.entity.InterestKeyword;
import com.part2.monew.entity.Keyword;
import com.part2.monew.entity.UserSubscriber;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserSubscriptionActivityResponse {

  private UUID id;
  private UUID interestId;
  private String interestName;
  private List<String> interestKeywords;
  private Long interestSubscriberCount;
  private Timestamp createdAt;

  public static UserSubscriptionActivityResponse of(Interest interest) {
    List<String> keywordNames = interest.getInterestKeywords().stream()
        .map(InterestKeyword::getKeyword)
        .map(Keyword::getName)
        .collect(Collectors.toList());

    return UserSubscriptionActivityResponse.builder()
        .interestId(interest.getId())
        .interestName(interest.getName())
        .interestKeywords(keywordNames)
        .interestSubscriberCount(interest.getSubscriberCount().longValue())
        .createdAt(interest.getCreatedAt())
        .build();
  }

}
