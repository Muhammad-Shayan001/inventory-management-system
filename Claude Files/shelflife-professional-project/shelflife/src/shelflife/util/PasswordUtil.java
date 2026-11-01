package shelflife.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Password hashing with salted PBKDF2 (HmacSHA256).
 *
 * <p>New hashes are stored as {@code pbkdf2$<iterations>$<hex>}. Hashes written by
 * older versions (a single round of salted SHA-256, plain hex) can still be
 * verified, and {@link #needsUpgrade(String)} tells the caller to re-hash them
 * after a successful login.</p>
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2$";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** Generates a random 128-bit salt as a hex string. */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    /** Hashes the password with the given salt using PBKDF2. */
    public static String hashPassword(String password, String salt) {
        return PREFIX + ITERATIONS + "$" + bytesToHex(pbkdf2(password, salt, ITERATIONS));
    }

    /** Checks a password against a stored hash (new PBKDF2 format or the legacy SHA-256 format). */
    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        if (expectedHash == null) {
            return false;
        }
        if (expectedHash.startsWith(PREFIX)) {
            String[] parts = expectedHash.split("\\$");
            if (parts.length != 3) {
                return false;
            }
            int iterations;
            try {
                iterations = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return false;
            }
            return constantTimeEquals(bytesToHex(pbkdf2(password, salt, iterations)), parts[2]);
        }
        return constantTimeEquals(legacySha256(password, salt), expectedHash);
    }

    /** True when the stored hash is in the old format or uses fewer iterations than today's default. */
    public static boolean needsUpgrade(String storedHash) {
        if (storedHash == null || !storedHash.startsWith(PREFIX)) {
            return true;
        }
        String[] parts = storedHash.split("\\$");
        try {
            return parts.length != 3 || Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static byte[] pbkdf2(String password, String salt, int iterations) {
        char[] chars = (password == null ? "" : password).toCharArray();
        byte[] saltBytes = (salt == null ? "" : salt).getBytes(StandardCharsets.UTF_8);
        PBEKeySpec spec = new PBEKeySpec(chars, saltBytes, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is not available on this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Format used before PBKDF2 was added. Kept only so old accounts can still sign in once. */
    private static String legacySha256(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt == null ? "" : salt).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(md.digest((password == null ? "" : password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.toLowerCase().getBytes(StandardCharsets.UTF_8),
                b.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
