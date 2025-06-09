package com.part2.monew.dto.request;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;

//검색 필터 조건
public record FilterDto(
    @RequestParam(value = "keyword", required = false)
    String keyword,

    @RequestParam(value = "interestId", required = false)
    UUID interestId,

    //출처 네이버 등
    @RequestParam(value = "sourceIn", required = false)
    List<String> sourceIn,

    @RequestParam(value = "publishDateFrom", required = false)
    Timestamp publishDateFrom,

    @RequestParam(value = "publishDateTo", required = false)
    Timestamp publishDateTo
) {

}
