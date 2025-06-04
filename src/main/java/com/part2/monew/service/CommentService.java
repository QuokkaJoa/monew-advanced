package com.part2.monew.service;

import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.response.CommentLikeReponse;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;

import java.util.UUID;

public interface CommentService {
    CursorResponse findCommentsByArticleId(CommentRequest commentRequest);

    CommentResponse create(CreateCommentRequest requeset);

    CommentResponse update(UUID id, String content);

    CommentLikeReponse likeComment(UUID id, UUID userId);

    void unlikeComment(UUID id, UUID userId);
}
