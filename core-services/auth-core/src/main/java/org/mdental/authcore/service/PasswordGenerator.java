package org.mdental.authcore.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordGenerator {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final String ALL = LOWER + UPPER + DIGITS + SPECIAL;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a strong random password with at least one character from each character class
     * @param length The length of the password (minimum 12)
     * @return A secure random password
     */
    public String generateStrongPassword(int length) {
        if (length < 12) {
            length = 12; // Enforce minimum length of 12
        }

        // Ensure we have at least one from each character class
        char[] password = new char[length];

        // Start with one from each required group
        password[0] = LOWER.charAt(random.nextInt(LOWER.length()));
        password[1] = UPPER.charAt(random.nextInt(UPPER.length()));
        password[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        password[3] = SPECIAL.charAt(random.nextInt(SPECIAL.length()));

        // Fill the rest with random characters from all groups
        for (int i = 4; i < length; i++) {
            password[i] = ALL.charAt(random.nextInt(ALL.length()));
        }

        // Shuffle to avoid predictable pattern
        for (int i = 0; i < password.length; i++) {
            int randomPosition = random.nextInt(password.length);
            char temp = password[i];
            password[i] = password[randomPosition];
            password[randomPosition] = temp;
        }

        return new String(password);
    }
}