package org.example.authchecking.service;

import org.example.authchecking.entity.AppUser;
import org.example.authchecking.entity.PasswordResetToken;
import org.example.authchecking.exception.InvalidPasswordResetTokenException;
import org.example.authchecking.repository.PasswordResetTokenRepository;
import org.example.authchecking.repository.UserRepository;
import org.example.authchecking.validators.PasswordValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PasswordResetService {

    private PasswordResetTokenRepository passwordResetTokenRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private PasswordValidator passwordValidator;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            PasswordValidator passwordValidator
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.passwordValidator = passwordValidator;
    }

    @Transactional
    public String createToken(AppUser user) {

        List<PasswordResetToken> oldTokens = passwordResetTokenRepository.findAllByUserIdAndUsedFalse(user.getId());

        oldTokens.forEach(token -> token.setUsed(true));

        passwordResetTokenRepository.saveAll(oldTokens);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken(
                token,
                user.getId(),
                Instant.now().plus(30, ChronoUnit.MINUTES)
        );

        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    public void resetPassword(String token, String newPassword) {

        passwordValidator.validatePassword(newPassword);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new InvalidPasswordResetTokenException("Reset token already used.");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidPasswordResetTokenException("Reset token expired");
        }

        AppUser user = userRepository.findById(resetToken.getUserId()).orElseThrow();

        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        refreshTokenService.revokeAllByUsername(user.getUsername());
    }
}
