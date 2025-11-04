package com.riverflow.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator for password strength
 * Checks if password contains:
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character
 */
public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {

    // At least one uppercase, one lowercase, one digit, one special character
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$"
    );

    @Override
    public void initialize(PasswordStrength constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true; // Let @NotBlank handle null/empty check
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}

