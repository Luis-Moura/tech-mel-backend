package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.UserNotFoundException;
import com.tech_mel.tech_mel.domain.port.input.UserUseCase;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import com.tech_mel.tech_mel.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {
    private final UserRepositoryPort userRepositoryPort;

    @Override
    public User getCurrentUser(String email) {
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
