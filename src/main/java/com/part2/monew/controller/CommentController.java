package com.part2.monew.controller;

import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<CursorResponse> findCommentsByArticleId(
            @Validated @ModelAttribute CommentRequest commentRequest
    ) {

        return ResponseEntity.status(HttpStatus.OK).body(commentService.findCommentsByArticleId(commentRequest));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Validated @RequestBody CreateCommentRequest commentRequest
    ){
        return ResponseEntity.status(HttpStatus.OK).body(commentService.create(commentRequest));
    }
}
