package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.port.input.UserUseCase;
import com.tech_mel.tech_mel.application.port.output.UserRepositoryPort;
import com.tech_mel.tech_mel.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {
    private final UserRepositoryPort userRepository;

    @Override
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
