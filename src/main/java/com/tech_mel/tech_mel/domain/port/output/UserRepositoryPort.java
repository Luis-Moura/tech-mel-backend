package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);

    User save(User user);

    Optional<User> findByVerificationToken(String token);
}