package com.tech_mel.tech_mel.infrastructure.security.oauth2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair implements Serializable {
    private String accessToken;
    private String refreshToken;
}