package org.example.authchecking.service;

import org.example.authchecking.entity.AppUser;
import org.example.authchecking.entity.EmailVerificationToken;
import org.example.authchecking.repository.EmailVerificationRepository;
import org.example.authchecking.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;

    public EmailVerificationService(
            EmailVerificationRepository emailVerificationRepository,
            UserRepository userRepository
    ) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.userRepository = userRepository;
    }

    public String createToken(AppUser user) {

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = new EmailVerificationToken(
                token,
                user.getId(),
                Instant.now().plus(30, ChronoUnit.MINUTES)
        );

        emailVerificationRepository.save(verificationToken);
        return token;
    }

    public ResponseEntity<String> verify(String token) {

        EmailVerificationToken verificationToken = emailVerificationRepository.findByToken(token).orElse(null);

        if (verificationToken == null) {
            return ResponseEntity.badRequest().body("Invalid verification token");
        }

        if (verificationToken.isUsed()) {
            return ResponseEntity.badRequest().body("Verification token already used");
        }

        if (verificationToken.getExpireAt().isBefore(Instant.now())) {
            return ResponseEntity.badRequest().body("Verification token expired");
        }

        AppUser user = userRepository.findById(verificationToken.getUserId()).orElseThrow();
        user.setVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationRepository.save(verificationToken);

        return ResponseEntity.ok("Email verified successfully");
    }
}
