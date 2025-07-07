package com.part2.monew.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part2.monew.controller.CommentController;
import com.part2.monew.controller.InterestController;
import com.part2.monew.controller.UserActivityController;
import com.part2.monew.controller.UserController;
import com.part2.monew.mapper.UserMapper;
import com.part2.monew.service.CommentService;
import com.part2.monew.service.InterestService;
import com.part2.monew.service.UserActivityService;
import com.part2.monew.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        CommentController.class,
        InterestController.class,
        UserActivityController.class,
        UserController.class
})
@ActiveProfiles("test")
public abstract class ControllerTestSupport {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected CommentService commentService;

    @MockBean
    protected InterestService interestService;

    @MockBean
    protected UserActivityService userActivityService;

    @MockitoBean
    protected UserService userService;

    @MockitoBean
    protected UserMapper userMapper;
}
