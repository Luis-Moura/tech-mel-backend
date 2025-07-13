package com.tech_mel.tech_mel.domain.port.output;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetCachePort {
    void storeResetToken(UUID token, UUID userId, Duration ttl);
    Optional<UUID> getUserIdByToken(UUID token);
    void deleteResetToken(UUID token);
}
