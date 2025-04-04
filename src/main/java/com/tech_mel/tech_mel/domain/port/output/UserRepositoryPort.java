package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    User save(User user);

    Optional<User> findByVerificationToken(String token);
}