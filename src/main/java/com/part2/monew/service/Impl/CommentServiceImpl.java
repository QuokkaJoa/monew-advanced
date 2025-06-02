package com.part2.monew.service.Impl;


import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.entity.CommentsManagement;
import com.part2.monew.repository.CommentRepository;
import com.part2.monew.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

    @Override
    public CursorResponse findCommentsByArticleId(CommentRequest commentRequest) {

        List<CommentsManagement> commentsManagements = commentRepository.findCommentsByArticleId(commentRequest.getArticleId(), commentRequest.getAfter(), commentRequest.getLimit());

        Long totalElements = commentRepository.totalCount(commentRequest.getArticleId());

        List<CommentResponse> commentReponses = commentsManagements.stream()
                .map(CommentResponse::of)
                .collect(Collectors.toList());

        return CursorResponse.of(commentReponses, totalElements);
    }

}
