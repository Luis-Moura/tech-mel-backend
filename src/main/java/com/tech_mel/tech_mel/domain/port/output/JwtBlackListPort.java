package com.tech_mel.tech_mel.domain.port.output;

public interface JwtBlackListPort {
    void addToBlacklist(String token);

    boolean isBlacklisted(String token);
}
