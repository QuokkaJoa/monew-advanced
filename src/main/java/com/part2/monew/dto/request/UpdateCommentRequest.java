package com.part2.monew.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateCommentRequest {

    @NotNull
    private String content;

    public UpdateCommentRequest(String content) {
        this.content = content;
    }
}
