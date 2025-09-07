package com.tech_mel.tech_mel.domain.port.output;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.tech_mel.tech_mel.domain.model.User;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    User save(User user);

    Optional<User> findByVerificationToken(String token);

    Page<User> findAllWithAvailableHives(Pageable pageable);
    
    // Novos métodos para administração
    Page<User> findAll(Pageable pageable);
    
    Page<User> findByRole(User.Role role, Pageable pageable);
    
    Page<User> findByIsActive(boolean isActive, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name, Pageable pageable);
    
    List<User> findByRole(User.Role role);
    
    List<User> findByIsPrimary(boolean isPrimary);
    
    List<User> findByCreatedAtAfter(LocalDateTime date);
    
    List<User> findByLastLoginBefore(LocalDateTime date);
    
    long countByRole(User.Role role);
    
    long countByIsActive(boolean isActive);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndIdNot(String email, UUID id);
    
    void deleteById(UUID id);
}