package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.User;

public interface UserUseCase {
    User getCurrentCommunUser(String email);

    User getCurrentTechnicianUser(String email);
}
