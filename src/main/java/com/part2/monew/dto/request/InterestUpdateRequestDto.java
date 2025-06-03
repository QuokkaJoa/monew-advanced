package com.part2.monew.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestUpdateRequestDto(
    @NotEmpty(message = "키워드는 최소 1개 이상 등록해야 합니다.")
    @Size(min = 1, max = 10, message = "키워드는 1개이상 10개 이하로 등록 할 수 있습니다.")
    List<String> keywords
) {

}
