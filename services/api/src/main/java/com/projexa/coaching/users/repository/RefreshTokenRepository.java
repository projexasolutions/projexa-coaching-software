package com.projexa.coaching.users.repository;

import com.projexa.coaching.users.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);
}