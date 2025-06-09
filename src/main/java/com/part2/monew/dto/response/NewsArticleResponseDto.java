package com.part2.monew.dto.response;

import java.sql.Timestamp;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleResponseDto {

    private UUID id;
    private String source;
    private String sourceUrl;
    private String title;
    private Timestamp publishDate;
    private String summary;
    private Long commentCount;
    private Long viewCount;
    private Boolean viewedByMe;

} 