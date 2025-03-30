package com.tech_mel.tech_mel.application.port.input;

import com.tech_mel.tech_mel.domain.model.User;

public interface AuthUseCase {
    String authenticateUser(String email, String password);

    void registerUser(String email, String password, String name);

    boolean verifyEmail(String token);

    String generateVerificationToken(User user);

    String refreshToken(String refreshToken);

    User findUserByEmail(String email);
}