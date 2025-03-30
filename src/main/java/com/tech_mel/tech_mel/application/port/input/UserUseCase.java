package com.tech_mel.tech_mel.application.port.input;

import com.tech_mel.tech_mel.domain.model.User;

public interface UserUseCase {
    User getCurrentUser(String email);
}
