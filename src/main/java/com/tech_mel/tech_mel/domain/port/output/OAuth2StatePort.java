package com.tech_mel.tech_mel.domain.port.output;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface OAuth2StatePort {
    void storeTokenPair(UUID stateId, Object tokenPair, Duration ttl);
    Optional<Object> getTokenPair(UUID stateId);
    void deleteState(UUID stateId);
}
