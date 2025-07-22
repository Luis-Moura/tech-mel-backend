package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import com.tech_mel.tech_mel.domain.model.Threshold;
import com.tech_mel.tech_mel.domain.port.output.ThresholdRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.ThresholdEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.ThresholdMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.ThresholdJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ThresholdRepositoryAdapter implements ThresholdRepositoryPort {
    private final ThresholdMapper thresholdMapper;
    private final ThresholdJpaRepository repository;

    @Override
    public Threshold save(Threshold threshold) {
        ThresholdEntity thresholdEntity = thresholdMapper.toEntity(threshold);

        ThresholdEntity savedEntity = repository.save(thresholdEntity);

        return thresholdMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Threshold> findById(UUID thresholdId) {
        return repository.findById(thresholdId)
                .map(thresholdMapper::toDomain);
    }

    @Override
    public Optional<Threshold> findByHiveId(UUID hiveId) {
        return repository.findByHiveId(hiveId)
                .map(thresholdMapper::toDomain);
    }
}
