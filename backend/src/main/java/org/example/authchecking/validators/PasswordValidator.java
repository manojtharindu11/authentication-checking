package org.example.authchecking.validators;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {
    public void validatePassword(String password) {

        if (password == null ||
                password.length() < 8) {

            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters"
            );
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException(
                    "Password must contain an uppercase letter"
            );
        }

        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException(
                    "Password must contain a lowercase letter"
            );
        }

        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Password must contain a number"
            );
        }
    }
}
