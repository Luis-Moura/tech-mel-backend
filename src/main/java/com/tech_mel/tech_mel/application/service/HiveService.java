package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.BadRequestException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.HiveUseCase;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.CreateHiveRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HiveService implements HiveUseCase {
    private final HiveRepositoryPort hiveRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    @Transactional
    public Hive createHive(CreateHiveRequest request) {
        User owner = userRepositoryPort.findById(request.ownerId())
                .orElseThrow(() -> new NotFoundException("Usuário dono da colmeia não encontrado"));

        if (owner.getAvailableHives() <= 0) {
            throw new BadRequestException("Usuário não possui colmeias disponíveis.");
        }

        Hive hive = Hive.builder()
                .name(request.name())
                .location(request.location())
                .apiKey(UUID.randomUUID().toString())
                .hiveStatus(Hive.HiveStatus.INACTIVE)
                .owner(owner)
                .build();

        Hive savedHive = hiveRepositoryPort.save(hive);

        owner.setAvailableHives(owner.getAvailableHives() - 1);
        userRepositoryPort.save(owner);

        return savedHive;
    }

    @Override
    public Page<Hive> listHivesByOwner(UUID ownerId, Pageable pageable) {
        Optional<User> user = userRepositoryPort.findById(ownerId);

        if (user.isEmpty()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        return hiveRepositoryPort.findByOwnerId(ownerId, pageable);
    }

    @Override
    public Optional<Hive> getHiveById(UUID hiveId) {
        return Optional.empty();
    }

    @Override
    public void updateApiKey(UUID hiveId, String newApiKey) {

    }

    @Override
    public void updateHiveStatus(UUID hiveId, Hive.HiveStatus hiveStatus) {

    }

    @Override
    public void deleteHive(UUID hiveId) {

    }
}
