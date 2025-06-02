package com.part2.monew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
    void test() throws Exception {
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
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
