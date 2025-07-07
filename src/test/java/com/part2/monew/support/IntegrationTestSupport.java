package com.part2.monew.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.CommentService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;


public abstract class IntegrationTestSupport extends ConfigurationTestSupport{
    @Autowired
    protected EntityManager em;

    @Autowired
    protected CommentService commentService;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

}
