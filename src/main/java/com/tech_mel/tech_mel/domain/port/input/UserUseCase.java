package com.tech_mel.tech_mel.domain.port.input;

import java.util.UUID;
import com.tech_mel.tech_mel.domain.model.User;

public interface UserUseCase {
    User getCurrentUser(UUID userId);

    void softDeleteUser(UUID id);

    void changePassword(String email, String newPassword, String oldPassword);
}
