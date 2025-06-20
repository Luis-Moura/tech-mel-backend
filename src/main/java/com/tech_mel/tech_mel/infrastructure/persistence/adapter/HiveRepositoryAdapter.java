package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.HiveEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.HiveMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.HiveJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HiveRepositoryAdapter implements HiveRepositoryPort {
    private final HiveMapper hiveMapper;
    private final HiveJpaRepository repository;

    @Override
    public Hive save(Hive hive) {
        HiveEntity entity = hiveMapper.toEntity(hive);
        HiveEntity savedEntity = repository.save(entity);
        return hiveMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Hive> findById(UUID hiveId) {
        return repository.findById(hiveId)
                .map(hiveMapper::toDomain);
    }

    @Override
    public List<Hive> findByOwnerId(UUID ownerId) {
        return repository.findByOwner_Id(ownerId).stream()
                .map(hiveMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID hiveId) {
        repository.deleteById(hiveId);
    }

    @Override
    public void updateApiKey(UUID hiveId, String apiKey) {
        repository.findById(hiveId).ifPresent(entity -> {
            entity.setApiKey(apiKey);
            repository.save(entity);
        });
    }

    @Override
    public void updateStatus(UUID hiveId, Hive.HiveStatus newStatus) {
        repository.findById(hiveId).ifPresent(entity -> {
            entity.setHiveStatus(newStatus);
            repository.save(entity);
        });
    }
}
