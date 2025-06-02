package com.part2.monew.controller;

import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<CursorResponse> findCommentsByArticleId(
            @Validated @ModelAttribute CommentRequest commentRequest
    ) {

        CursorResponse cursorResponse = commentService.findCommentsByArticleId(commentRequest);
        
        return ResponseEntity.status(HttpStatus.OK).body(cursorResponse);
    }

}
