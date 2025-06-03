package com.part2.monew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestRegisterRequestDto(
    @NotBlank(message = "관심사 이름은 필수입니다.")
    @Size(min = 1, max = 50, message = "관심사 이름은 1자 이상 50자 이하로 입력해주세요.")
    String name,
    @NotEmpty(message = "키워드는 최소 1개 이상 등록해야 합니다.")
    @Size(min = 1, max = 10, message = "키워드는 1개 이상 10개 이하로 등록할 수 있습니다.")
    List<String> keywords
) {

}
