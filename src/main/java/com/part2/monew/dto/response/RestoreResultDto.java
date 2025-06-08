package com.part2.monew.dto.response;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreResultDto {
    
    private Timestamp restoreDate;
    private List<UUID> restoredArticleIds;
    private Long restoredArticleCount;
} 