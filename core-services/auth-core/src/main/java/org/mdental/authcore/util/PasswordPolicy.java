package org.mdental.authcore.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Service for password policy validation.
 */
@Component
public class PasswordPolicy {
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * Validate that a password meets the policy requirements.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if the password does not meet the requirements
     */
    public void validate(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        if (!HAS_UPPERCASE.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        if (!HAS_LOWERCASE.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        if (!HAS_DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        if (!HAS_SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}