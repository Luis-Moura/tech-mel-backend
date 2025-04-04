package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.ConflictException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.domain.port.input.UserUseCase;
import com.tech_mel.tech_mel.domain.port.output.EmailSenderPort;
import com.tech_mel.tech_mel.domain.port.output.JwtPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final JwtPort jwtPort;
    private final RefreshTokenUseCase refreshTokenUseCase;

    @Override
    public User getCurrentCommunUser(String email) {
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    @Override
    public User getCurrentTechnicianUser(String email) {
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    @Override
    public User getCurrentAdminUser(String email) {
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    @Override
    @Transactional
    public void softDeleteUser(UUID id) {
        User user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (!user.isEnabled()) {
            throw new ConflictException("Usuário já excluído");
        }

        user.setEmail(user.getEmail() + ".deleted" + "." + user.getId());
        user.setName(user.getName() + " (excluído)");
        user.setEnabled(false);
        user.setRole(null);

        refreshTokenUseCase.revokeAllUserTokens(user);

        emailSenderPort.sendUserDeletionEmail(user.getEmail(), user.getName());

        userRepositoryPort.save(user);
    }
}
