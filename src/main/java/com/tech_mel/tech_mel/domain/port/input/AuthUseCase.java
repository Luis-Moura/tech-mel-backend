package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.User;

import java.util.UUID;

public interface AuthUseCase {
    String authenticateUser(String email, String password);

    void registerUser(String email, String password, String name);

    void resendVerificationEmail(String email);

    void verifyEmail(String token);

    String generateVerificationToken(User user);

    String refreshToken(String refreshToken);

    User findUserByEmail(String email);

    void logout(String token);

    void requestPasswordReset(String email);

    void resetPassword(UUID token, String newPassword);
}