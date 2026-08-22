package org.example.authchecking.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.authchecking.dto.*;
import org.example.authchecking.entity.AppUser;
import org.example.authchecking.entity.AuthProvider;
import org.example.authchecking.repository.UserRepository;
import org.example.authchecking.security.CustomUserDetailsService;
import org.example.authchecking.security.JwtService;
import org.example.authchecking.service.EmailService;
import org.example.authchecking.service.EmailVerificationService;
import org.example.authchecking.service.PasswordResetService;
import org.example.authchecking.service.RefreshTokenService;
import org.example.authchecking.validators.PasswordValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final PasswordResetService passwordResetService;
    private final PasswordValidator passwordValidator;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtDecoder jwtDecoder,
            CustomUserDetailsService userDetailsService,
            RefreshTokenService refreshTokenService,
            EmailVerificationService emailVerificationService,
            EmailService emailService,
            PasswordResetService passwordResetService,
            PasswordValidator passwordValidator
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtDecoder = jwtDecoder;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationService = emailVerificationService;
        this.emailService = emailService;
        this.passwordResetService = passwordResetService;
        this.passwordValidator = passwordValidator;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().body("Username already exist");
        }

        passwordValidator.validatePassword(request.password());

        AppUser user = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                "USER"
        );

        user.setProvider(AuthProvider.LOCAL);

        AppUser savedUser = userRepository.save(user);

        String verificationToken = emailVerificationService.createToken(savedUser);
        String verificationLink = "http://localhost:8080/auth/verify-email?token=" + verificationToken;

        emailService.sendVerificationEmail(savedUser.getUsername(), verificationLink);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        AppUser user = userRepository.findByUsername(loginRequest.username()).orElseThrow();

        if (!user.isVerified()) {
            throw new DisabledException(
                    "Email is not verified"
            );
        }

        String accessToken = jwtService.generateAccessToken(authentication);

        String refreshToken = jwtService.generateRefreshToken(authentication);

        refreshTokenService.save(refreshToken, authentication.getName());

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)  //localhost only
                .sameSite("Lax")
                .path("/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        return ResponseEntity.ok(
                new LoginResponse(accessToken)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String oldRefreshToken,
            HttpServletResponse response
    ) {
        if (oldRefreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        Jwt jwt;

        try {
            jwt = jwtDecoder.decode(oldRefreshToken);
        } catch (JwtException e) {
            return ResponseEntity.status(401).build();
        }

        String type = jwt.getClaimAsString("type");

        if (!"refresh".equals(type)) {
            return ResponseEntity.status(401).build();
        }

        try {
            refreshTokenService.validate(oldRefreshToken);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build();
        }

        String username = jwt.getSubject();

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        // revoke old refresh token
        refreshTokenService.revoke(oldRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(authentication);
        String newRefreshToken = jwtService.generateRefreshToken(authentication);

        refreshTokenService.save(newRefreshToken, username);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(
                new LoginResponse(newAccessToken)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(0)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                deleteCookie.toString()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        return emailVerificationService.verify(token);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {

        userRepository.findByUsername(request.email())
                .ifPresent(user -> {
                    String token = passwordResetService.createToken(user);

                    String resetLink = "http://localhost:5173/reset-password?token="+token;

                    emailService.sendPasswordResetEmail(user.getUsername(), resetLink);
                });

        return ResponseEntity.ok(
                "If an account exists for that email, a reset link has been sent."
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());

        return ResponseEntity.ok("Password reset successfully");
    }
}
