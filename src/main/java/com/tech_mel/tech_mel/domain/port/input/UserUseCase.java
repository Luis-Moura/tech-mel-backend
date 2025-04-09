package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.User;

import java.util.UUID;

public interface UserUseCase {
    User getCurrentUser(String email);

    void softDeleteUser(UUID id);
}
