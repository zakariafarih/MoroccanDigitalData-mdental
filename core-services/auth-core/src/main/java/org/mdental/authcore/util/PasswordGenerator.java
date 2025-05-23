package org.mdental.authcore.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility for generating secure passwords.
 */
@Component
public class PasswordGenerator {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final String ALL = LOWER + UPPER + DIGITS + SPECIAL;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generate a strong random password.
     *
     * @return a secure random password
     */
    public String generateStrongPassword() {
        return generateStrongPassword(16);
    }

    /**
     * Generate a strong random password with specified length.
     *
     * @param length the desired password length (minimum 12)
     * @return a secure random password
     */
    public String generateStrongPassword(int length) {
        if (length < 12) {
            length = 12;
        }

        char[] password = new char[length];

        // Ensure at least one character from each required class
        password[0] = LOWER.charAt(random.nextInt(LOWER.length()));
        password[1] = UPPER.charAt(random.nextInt(UPPER.length()));
        password[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        password[3] = SPECIAL.charAt(random.nextInt(SPECIAL.length()));

        // Fill the rest with random characters
        for (int i = 4; i < length; i++) {
            password[i] = ALL.charAt(random.nextInt(ALL.length()));
        }

        // Shuffle to avoid predictable pattern
        for (int i = 0; i < password.length; i++) {
            int j = random.nextInt(password.length);
            char temp = password[i];
            password[i] = password[j];
            password[j] = temp;
        }

        return new String(password);
    }
}
