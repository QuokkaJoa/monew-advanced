package com.part2.monew.service;

import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;

public interface CommentService {
    CursorResponse findCommentsByArticleId(CommentRequest commentRequest);

    CommentResponse create(CreateCommentRequest requeset);
}
