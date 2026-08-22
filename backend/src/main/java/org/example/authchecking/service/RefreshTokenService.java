package org.example.authchecking.service;

import org.example.authchecking.entity.RefreshToken;
import org.example.authchecking.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void save(String token, String username) {

        RefreshToken refreshToken = new RefreshToken(
                token,
                username,
                Instant.now().plus(7, ChronoUnit.DAYS)
        );

        refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validate(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (refreshToken.isRevoke()) {
            throw new RuntimeException("Refresh token is revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token is expired");
        }

        return refreshToken;
    }

    public void revoke(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoke(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Transactional
    public void revokeAllByUsername(String username) {

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUsername(username);

        tokens.forEach(token -> token.setRevoke(true));

        refreshTokenRepository.saveAll(tokens);
    }
}
