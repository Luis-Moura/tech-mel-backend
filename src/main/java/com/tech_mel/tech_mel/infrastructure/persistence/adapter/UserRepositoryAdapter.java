package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.UserEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.UserMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);
        return userMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByVerificationToken(String token) {
        return userJpaRepository.findByVerificationToken(token)
                .map(userMapper::toDomain);
    }
}