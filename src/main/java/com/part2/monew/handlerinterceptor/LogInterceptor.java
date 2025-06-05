package com.part2.monew.handlerinterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    public static final String LOG_ID = "logId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());

        String requestURI = request.getRequestURI();

        String userId = request.getHeader("monew-request-id");
        if (userId == null) {
            userId = UUID.randomUUID().toString();
        }
        request.setAttribute(LOG_ID, userId);

        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
        }



        log.info("REQUEST [{}] [{}] [{}]", userId, requestURI, handler);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");

        long duration = (startTime != null ? System.currentTimeMillis() - startTime : -1);

        String userId = (String) request.getAttribute(LOG_ID);

        int status     = response.getStatus();

        log.info("RESPONSE [{}] {} {} → status={} time={}ms", userId, request.getMethod(), request.getRequestURI(), status, duration);
    }
}