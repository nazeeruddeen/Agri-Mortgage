package com.employee.loan_system.repository;

import com.employee.loan_system.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUser_IdAndRevokedFalse(Long userId);
    void deleteByExpiresAtBefore(LocalDateTime time);
}
