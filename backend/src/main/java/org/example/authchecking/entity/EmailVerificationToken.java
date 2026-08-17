package org.example.authchecking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private long userId;

    @Column(nullable = false)
    private Instant expireAt;

    @Column(nullable = false)
    private boolean used;

    public EmailVerificationToken(
            String token,
            long userId,
            Instant expireAt
    ) {
        this.token = token;
        this.userId = userId;
        this.expireAt = expireAt;
        this.used = false;
    }
}
