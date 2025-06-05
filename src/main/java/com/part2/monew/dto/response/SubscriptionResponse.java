package com.part2.monew.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    Long interestSubscriberCount,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    Timestamp createdAt
) {

}
