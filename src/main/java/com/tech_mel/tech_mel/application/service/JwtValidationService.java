package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.domain.port.output.JwtBlackListPort;
import com.tech_mel.tech_mel.domain.port.output.JwtOperationsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtValidationService {
    private final JwtOperationsPort jwtOperationsPort;
    private final JwtBlackListPort jwtBlackListPort;

    public boolean validateToken(String token, String expectedType) {
        if (jwtBlackListPort.isBlacklisted(token)) {
            return false;
        }

        return jwtOperationsPort.isTokenValid(token, expectedType);
    }
}
