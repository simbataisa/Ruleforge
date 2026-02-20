package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.UserDto;
import io.ruleforge.auth.service.UserService;
import io.ruleforge.common.dto.ApiResponse;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserDto user = userService.getCurrentUser(userId);
        return ApiResponse.ok(user);
    }
}
