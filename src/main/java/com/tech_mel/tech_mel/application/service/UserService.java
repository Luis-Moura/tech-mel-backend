package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.ConflictException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.domain.port.input.UserUseCase;
import com.tech_mel.tech_mel.domain.port.output.EmailSenderPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getCurrentUser(String email) {
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    @Override
    @Transactional
    public void softDeleteUser(UUID id) {
        log.info("Tentativa de exclusão lógica do usuário com ID: {}", id);
        User user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (!user.isEnabled()) {
            throw new ConflictException("Usuário já excluído");
        }

        emailSenderPort.sendUserDeletionEmail(user.getEmail(), user.getName());

        user.setEmail(user.getEmail() + ".deleted" + "." + user.getId());
        user.setName(user.getName() + " (excluído)");
        user.setEnabled(false);
        user.setRole(null);

        refreshTokenUseCase.revokeAllUserTokens(user);

        userRepositoryPort.save(user);
    }

    @Override
    public void changePassword(String email, String newPassword, String oldPassword) {
        log.info("Tentativa de alteração de senha do usuário com email: {}", email);
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
            log.warn("Tentativa de alteração de senha para conta Google: {}", email);
            throw new ConflictException("Usuários com login pelo Google não podem alterar a senha");
        }

        if (newPassword.equals(oldPassword)) {
            log.warn("Tentativa de alteração de senha com a mesma senha atual: {}", email);
            throw new ConflictException("A nova senha não pode ser igual à senha atual");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Senha atual incorreta na tentativa de alteração para: {}", email);
            throw new ConflictException("Senha atual incorreta");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepositoryPort.save(user);
        log.info("Senha alterada com sucesso para: {}", email);
    }
}
