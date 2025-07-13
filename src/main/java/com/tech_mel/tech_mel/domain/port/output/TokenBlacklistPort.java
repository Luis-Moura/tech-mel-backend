package com.tech_mel.tech_mel.domain.port.output;

public interface TokenBlacklistPort {
    void addToBlacklist(String token);
    boolean isBlacklisted(String token);
}
