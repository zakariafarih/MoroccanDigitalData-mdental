package org.mdental.security.password;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;
import java.util.regex.Pattern;

/**

 Service for password operations including hashing, verification, and strength validation.
 */
@RequiredArgsConstructor
public class PasswordService {

    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final PasswordEncoder passwordEncoder;

    /**

     Hashes a raw password using BCrypt

     @param rawPassword The password to hash (not null)

     @return The hashed password

     @throws NullPointerException if rawPassword is null
     */
    public String hash(char[] rawPassword) {
        Objects.requireNonNull(rawPassword, "Password cannot be null");

        String hashed = passwordEncoder.encode(new String(rawPassword));
        java.util.Arrays.fill(rawPassword, '\0');   // wipe sensitive data
        return hashed;
    }

    /**

     Verifies if a raw password matches a hashed password

     @param rawPassword The raw password to check

     @param hashedPassword The hashed password to compare against

     @return true if the raw password matches the hashed password
     */
    public boolean matches(char[] rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(new String(rawPassword), hashedPassword);
        } finally {
            // Wipe sensitive data
            java.util.Arrays.fill(rawPassword, '\0');
        }
    }

    /**

     Validates if a password meets minimum strength requirements

     <p>Requirements:
     <ul>
     <li>At least 8 characters long</li>
     <li>Contains at least one uppercase letter</li>
     <li>Contains at least one lowercase letter</li>
     <li>Contains at least one digit</li>
     </ul>
     @param rawPassword The password to validate

     @throws IllegalArgumentException if the password doesn't meet requirements

     @throws NullPointerException if rawPassword is null
     */
    public void validateStrength(char[] rawPassword) {
        Objects.requireNonNull(rawPassword, "Password cannot be null");

        String password = new String(rawPassword);

        if (password.length() < MIN_PASSWORD_LENGTH) {
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
    }
}