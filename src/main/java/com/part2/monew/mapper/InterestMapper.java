package com.part2.monew.mapper;

import com.part2.monew.dto.request.InterestRegisterRequestDto;
import com.part2.monew.dto.response.InterestDto;
import com.part2.monew.entity.Interest;
import com.part2.monew.entity.InterestKeyword;
import com.part2.monew.entity.Keyword;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface InterestMapper {

  @Mapping(source = "id", target = "id")
  @Mapping(source = "name", target = "name")
  @Mapping(source = "interest.subscriberCount", target = "subscriberCount")
  @Mapping(source = "interest.interestKeywords", target = "keywords", qualifiedByName = "interestKeywordsToKeywordNames")
  InterestDto toDto(Interest interest, boolean subscriberByMe);

  @Mapping(target = "name", source = "name")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "subscriberCount", ignore = true)
  @Mapping(target = "interestKeywords", ignore = true)
  @Mapping(target = "interestNewsArticle", ignore = true)
  @Mapping(target = "userSubscriber", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Interest fromRegisterRequestDto(InterestRegisterRequestDto dto);

  @Named("interestKeywordsToKeywordNames")
  default List<String> interestKeywordsToKeywordNames(List<InterestKeyword> interestKeywords) {
    if (interestKeywords == null || interestKeywords.isEmpty()) {
      return Collections.emptyList();
    }
    return interestKeywords.stream()
        .map(InterestKeyword::getKeyword)
        .map(Keyword::getName)
        .collect(Collectors.toList());
  }
}
