package com.tech_mel.tech_mel.infrastructure.api.controller;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.UserUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.users.ChangePasswordRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.users.UserResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;
    private final AuthenticationUtil authenticationUtil;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        // Usar o UUID como principal para busca direta
        UUID userId = authenticationUtil.getCurrentUserId();
        
        User currentUser = userUseCase.getCurrentUser(userId);

        UserResponse userResponse = UserResponse.builder()
                .id(currentUser.getId())
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .role(currentUser.getRole())
                .emailVerified(currentUser.isEmailVerified())
                .createdAt(currentUser.getCreatedAt())
                .lastLogin(currentUser.getLastLogin())
                .build();

        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // Usar o email das claims do JWT
        String email = authenticationUtil.getCurrentUserEmail();

        userUseCase.changePassword(email, request.newPassword(), request.oldPassword());
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso!"));
    }

    @PutMapping("/delete")
    public ResponseEntity<Void> softDeleteUser() {
        // Usar o UUID como principal para busca direta
        UUID userId = authenticationUtil.getCurrentUserId();
        
        userUseCase.softDeleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}