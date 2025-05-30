package com.part2.monew.controller;

import com.part2.monew.dto.UserInfoRequest;
//import com.part2.monew.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
//    private final UserService userService;
//
//    @PostMapping("/validate")
//    public ResponseEntity<Void> validateUserInfo(@RequestBody @Valid UserInfoRequest request){
//        return ResponseEntity.ok().build();
//    }
}
