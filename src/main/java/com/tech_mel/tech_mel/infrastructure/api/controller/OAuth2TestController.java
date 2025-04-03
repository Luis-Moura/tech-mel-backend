package com.tech_mel.tech_mel.infrastructure.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2TestController {

    @GetMapping("/oauth2/callback")
    public String handleCallback(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String refreshToken,
            @RequestParam(required = false) String error
    ) {
        if (error != null) {
            return "Erro: " + error;
        }

        return "Autenticação bem-sucedida!<br>" +
                "Access Token: " + token + "<br>" +
                "Refresh Token: " + refreshToken;
    }
}