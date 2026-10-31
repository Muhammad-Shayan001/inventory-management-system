package shelflife.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility for secure password hashing and verification using salted SHA-256.
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * Generates a random cryptographic salt hex string.
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Hashes a plaintext password with the provided salt using SHA-256.
     */
    public static String hashPassword(String password, String salt) {
        if (password == null) {
            password = "";
        }
        if (salt == null) {
            salt = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies that a plaintext password matches the stored salted hash.
     */
    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        if (expectedHash == null) {
            return false;
        }
        String calculated = hashPassword(password, salt);
        return calculated.equalsIgnoreCase(expectedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
