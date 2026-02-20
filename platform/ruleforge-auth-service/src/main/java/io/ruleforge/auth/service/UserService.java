package io.ruleforge.auth.service;

import io.ruleforge.auth.dto.UserDto;
import io.ruleforge.auth.entity.UserEntity;
import io.ruleforge.auth.repository.UserRepository;
import io.ruleforge.common.exception.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId.toString()));
        return toDto(user);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, String fullName) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId.toString()));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        user = userRepository.save(user);
        return toDto(user);
    }

    private UserDto toDto(UserEntity user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName());
    }
}
