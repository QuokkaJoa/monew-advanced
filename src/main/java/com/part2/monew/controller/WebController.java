package com.part2.monew.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {

    // 확장자가 없는 최상위 경로 (예: /about, /dashboard)
    @RequestMapping("/{path:[^\\.]*}")
    public String forwardRoot() {
        return "forward:/index.html";
    }

    // 확장자가 없는 하위 경로 (예: /user/profile, /posts/123)
    @RequestMapping("/**/{path:[^\\.]*}")
    public String forwardSubPaths() {
        return "forward:/index.html";
    }
}
