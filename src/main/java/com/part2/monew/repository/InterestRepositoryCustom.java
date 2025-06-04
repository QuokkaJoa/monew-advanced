package com.part2.monew.repository;

import com.part2.monew.dto.response.CursorPageResponse;
import com.part2.monew.dto.response.InterestDto;
import java.util.UUID;

public interface InterestRepositoryCustom {
  CursorPageResponse<InterestDto> searchInterestsWithQueryDsl(
      String keywordSearchTerm,
      String orderByField,
      String direction,
      String primaryCursorValue,
      String secondaryCursorValue,
      int limit,
      UUID requestUserId
  );
}
