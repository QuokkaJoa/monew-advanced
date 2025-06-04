package com.part2.monew.dto.response;

import com.part2.monew.entity.Interest;
import com.part2.monew.entity.InterestKeyword;
import com.part2.monew.entity.UserSubscriber;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserSubscriberResponse {

  private UUID id;
  private UUID interestId;
  private String interestName;
  private List<String> interestKeywords;
  private Long interestSubscriberCount;
  private Timestamp createdAt;

  public static UserSubscriberResponse of(UserSubscriber subscriber) {
    Interest interest = subscriber.getInterest();
    List<String> keywordNames = interest.getInterestKeywords().stream()
        .map(InterestKeyword::getKeyword)
        .map(k -> k.getName())
        .collect(Collectors.toList());

    return UserSubscriberResponse.builder()
        .id(subscriber.getId())
        .interestId(interest.getId())
        .interestName(interest.getName())
        .interestKeywords(keywordNames)
        .interestSubscriberCount(interest.getSubscriberCount().longValue())
        .createdAt(subscriber.getCreatedAt())
        .build();
  }
}
