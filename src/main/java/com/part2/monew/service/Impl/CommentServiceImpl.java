package com.part2.monew.service.Impl;


import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.response.CommentLikeReponse;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.entity.CommentLike;
import com.part2.monew.entity.CommentsManagement;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.User;
import com.part2.monew.repository.CommentLikeRepository;
import com.part2.monew.repository.CommentRepository;
import com.part2.monew.repository.NewsArticleRepository;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
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
    @Transactional
    public CommentResponse create(CreateCommentRequest requeset) {
        User user = userRepository.findById(requeset.getUserId())
                .orElseThrow( () ->  new NoSuchElementException("user with id " + requeset.getUserId() + " not found"));


        NewsArticle article = articleRepository.findById(requeset.getArticleId())
                .orElseThrow( () ->  new NoSuchElementException("article with id " + requeset.getUserId() + " not found"));

        CommentsManagement comment = CommentsManagement.create(user, article, requeset.getContent(), 0);

        CommentsManagement saveComment = commentRepository.saveAndFlush(comment);

        return CommentResponse.of(saveComment);

    }

    @Override
    @Transactional
    public CommentResponse update(UUID id, String content) {
        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow( () ->  new NoSuchElementException("comment with id " + id + " not found"));

        commentsManagement.update(content);

        return CommentResponse.of(commentsManagement);
    }

    @Override
    @Transactional
    public CommentLikeReponse likeComment(UUID id, UUID userId) {
        Optional<CommentLike> existingLikeOpt = commentLikeRepository.findByCommentsManagement_IdAndUser_Id(id, userId);

        existingLikeOpt.ifPresent(cl -> {
            throw new IllegalArgumentException("이미 좋아요를 눌렀습니다.");
        });


        User user = userRepository.findById(userId)
                .orElseThrow( () ->  new NoSuchElementException("user with id " + userId + " not found"));

        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow( () ->  new NoSuchElementException("comment with id " + id + " not found"));


        CommentLike commentLike = CommentLike.create(user, commentsManagement);

        CommentLike saveComment = commentLikeRepository.saveAndFlush(commentLike);

        int totalLike = commentTotalLike(commentsManagement);

        commentsManagement.updateTotalCount(totalLike);

        return CommentLikeReponse.of(commentsManagement, saveComment);
    }

    @Override
    @Transactional
    public void unlikeComment(UUID id, UUID userId) {
        CommentLike commentLike = commentLikeRepository.findByCommentsManagement_IdAndUser_Id(id, userId)
                .orElseThrow(( ) -> new NoSuchElementException("좋아요 취소를 이미 눌렀습니다."));

        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow( () ->  new NoSuchElementException("comment with id " + id + " not found"));


        commentLikeRepository.deleteById(commentLike.getId());

        int totalLike = commentTotalLike(commentsManagement);

        commentsManagement.updateTotalCount(totalLike);
    }

    @Override
    @Transactional
    public void deleteComment(UUID id) {
        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow( () ->  new NoSuchElementException("comment with id " + id + " not found"));

        commentsManagement.delete();

    }

    @Override
    @Transactional
    public void hardDeleteComment(UUID id) {
        CommentsManagement commentsManagement = commentRepository.findById(id)
                .orElseThrow( () ->  new NoSuchElementException("comment with id " + id + " not found"));

        if(commentsManagement.isActive()){
            throw new RuntimeException("isisActive " + commentsManagement.isActive() +"는 삭제를 진행할 수 없습니다.");
        }

        commentRepository.deleteById(id);
    }

    private int commentTotalLike(CommentsManagement commentsManagement) {
        return commentLikeRepository.findAllByCommentsManagement(commentsManagement).size();
    }

}
