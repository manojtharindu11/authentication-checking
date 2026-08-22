package org.example.authchecking.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
