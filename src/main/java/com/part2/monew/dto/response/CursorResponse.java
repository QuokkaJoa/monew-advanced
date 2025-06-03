package com.part2.monew.dto.response;

import com.part2.monew.entity.CommentsManagement;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CursorResponse {
    private List<CommentResponse> content = new ArrayList<>();
    private String nextCursor;
    private String nextAfter;
    private int size;
    private Long totalElements;
    private Boolean hasNext;

    @Builder
    protected CursorResponse(List<CommentResponse> content, String nextCursor, String nextAfter, int size, Long totalElements, Boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.nextAfter = nextAfter;
        this.size = size;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    public static CursorResponse of(List<CommentResponse> comments, Long totalElements) {
        boolean hasNext = hasNext(comments);
        List<CommentResponse> content = hasNext ? comments.subList(0, 5) : comments;

        return CursorResponse.builder()
                .content(content)
                .nextCursor(getNextCursor(content))
                .nextAfter(getNextCursor(content))
                .size(content.size())
                .totalElements(totalElements)
                .hasNext(hasNext(comments))
                .build();
    }

    private static boolean hasNext(List<CommentResponse> comments) {
        return comments.size() > 5;
    }

    private static String getNextCursor(List<CommentResponse> content) {
        return content.isEmpty() ? null : content.get(content.size() - 1).getCreatedAt().toString();
    }


}
