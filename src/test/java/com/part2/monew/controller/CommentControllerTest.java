package com.part2.monew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.request.UpdateCommentRequest;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @DisplayName("댓글 목록을 조회한다.")
    @Test
    void findCommentsByArticleId() throws Exception {
        // given
        CommentRequest commentRequest = CommentRequest.builder()
                .articleId(UUID.randomUUID())
                .orderBy("createdAt")
                .direction("DESC")
                .limit(10)
                .cursor(null)
                .after(null)
                .requestUserId(null)
                .build();

        // when // then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/comments")
                        .param("articleId", commentRequest.getArticleId().toString())
                        .param("orderBy", commentRequest.getOrderBy())
                        .param("direction", commentRequest.getDirection())
                        .param("limit", String.valueOf(commentRequest.getLimit())))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }


    @DisplayName("댓글을 생성한다.")
    @Test
    void createComment() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        String content = "테스트 댓글 내용";

        CreateCommentRequest request = CreateCommentRequest.create(userId, articleId, content);

        // when // then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/comments")
                .content(objectMapper.writeValueAsString(request))
                .contentType((MediaType.APPLICATION_JSON))
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

    }

    @DisplayName("댓글을 업데이트한다.")
    @Test
    void updateComment() throws Exception {
        // given
        UUID commentId = UUID.randomUUID();
        String content = "업데이트";

        UpdateCommentRequest request = new UpdateCommentRequest(content);

        CommentResponse fakeResponse = CommentResponse.builder()
                .id(commentId)
                .userId(UUID.randomUUID())
                .userNickname("tester")
                .articleId(UUID.randomUUID())
                .content(content)
                .likeCount(0)
                .likedByMe(false)
                .createdAt(Timestamp.from(Instant.now()))
                .build();

        when(commentService.update(commentId, content)).thenReturn(fakeResponse);

        // when // then
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/comments/{commentId}", commentId)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType((MediaType.APPLICATION_JSON))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.content").value(fakeResponse.getContent()));

    }
}
