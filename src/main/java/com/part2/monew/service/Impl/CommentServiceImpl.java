package com.part2.monew.service.Impl;


import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.entity.CommentsManagement;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.User;
import com.part2.monew.repository.CommentRepository;
import com.part2.monew.repository.NewsArticleRepository;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NewsArticleRepository articleRepository;

    @Override
    public CursorResponse findCommentsByArticleId(CommentRequest commentRequest) {

        List<CommentsManagement> commentsManagements = commentRepository.findCommentsByArticleId(commentRequest.getArticleId(), commentRequest.getAfter(), commentRequest.getLimit());

        Long totalElements = commentRepository.totalCount(commentRequest.getArticleId());

        List<CommentResponse> commentReponses = commentsManagements.stream()
                .map(CommentResponse::of)
                .collect(Collectors.toList());

        return CursorResponse.of(commentReponses, totalElements);
    }

    @Override
    public CommentResponse create(CreateCommentRequest requeset) {
        User user = userRepository.findById(requeset.getUserId())
                .orElseThrow( () ->  new NoSuchElementException("user with id " + requeset.getUserId() + " not found"));


        NewsArticle article = articleRepository.findById(requeset.getArticleId())
                .orElseThrow( () ->  new NoSuchElementException("article with id " + requeset.getUserId() + " not found"));

        CommentsManagement comment = CommentsManagement.create(user, article, requeset.getContent(), 0);

        CommentsManagement saveComment = commentRepository.saveAndFlush(comment);

        return CommentResponse.of(saveComment);

    }

}
