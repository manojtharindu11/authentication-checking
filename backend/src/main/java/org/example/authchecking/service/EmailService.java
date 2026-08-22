package org.example.authchecking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String verificationLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(to);

        message.setSubject("Verify your email");
        message.setText(
                """
                Welcome!
                
                Please verify your email by opening this link:
                
                %s
                
                This link will expire shortly.
                """.formatted(verificationLink)
        );

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(to);

        message.setSubject("Reset your password");

        message.setText(
                """
                We received a request to reset your password.
                
                Use the link below:
                
                %s
                
                This link will expire in 30 minutes.
                
                If you did not request a password reset,
                you can ignore this email.
                """.formatted(resetLink)
        );

        mailSender.send(message);
    }
}
