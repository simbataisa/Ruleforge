package io.ruleforge.auth.controller;

import io.ruleforge.auth.dto.ChangePasswordRequest;
import io.ruleforge.auth.dto.UpdateProfileRequest;
import io.ruleforge.auth.dto.UserDto;
import io.ruleforge.auth.service.UserService;
import io.ruleforge.common.dto.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/me")
    public ApiResponse<UserDto> updateProfile(Authentication authentication,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserDto user = userService.updateProfile(userId, request.fullName());
        return ApiResponse.ok(user);
    }

    @PostMapping("/me/change-password")
    public ApiResponse<Void> changePassword(Authentication authentication,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ApiResponse.ok(null);
    }
}
