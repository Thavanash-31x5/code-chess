package app.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.security.MessageDigest;

public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT = "pbkdf2-sha256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        char[] passwordCharacters = password.toCharArray();

        try {
            PBEKeySpec keySpec = new PBEKeySpec(
                    passwordCharacters, salt, ITERATIONS, KEY_LENGTH_BITS);
            byte[] hash = SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();

            return String.join(
                    "$",
                    FORMAT,
                    Integer.toString(ITERATIONS),
                    Base64.getUrlEncoder().withoutPadding().encodeToString(salt),
                    Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to hash password", exception);
        } finally {
            java.util.Arrays.fill(passwordCharacters, '\0');
        }
    }


    public static boolean verify(String password, String storedHash) {
    if (password == null || storedHash == null) {
        return false;
    }

    String[] parts = storedHash.split("\\$", -1);

    if (parts.length != 4 || !FORMAT.equals(parts[0])) {
        return false;
    }

    int iterations;

    try {
        iterations = Integer.parseInt(parts[1]);
    } catch (NumberFormatException exception) {
        return false;
    }

    if (iterations <= 0) {
        return false;
    }

    byte[] salt;
    byte[] expectedHash;

    try {
        salt = Base64.getUrlDecoder().decode(parts[2]);
        expectedHash = Base64.getUrlDecoder().decode(parts[3]);
    } catch (IllegalArgumentException exception) {
        return false;
    }

    char[] passwordCharacters = password.toCharArray();
    PBEKeySpec keySpec = new PBEKeySpec(
            passwordCharacters,
            salt,
            iterations,
            KEY_LENGTH_BITS
    );

    try {
        byte[] actualHash = SecretKeyFactory.getInstance(ALGORITHM)
                .generateSecret(keySpec)
                .getEncoded();

        return MessageDigest.isEqual(actualHash, expectedHash);
    } catch (GeneralSecurityException exception) {
        return false;
    } finally {
        keySpec.clearPassword();
        java.util.Arrays.fill(passwordCharacters, '\0');
    }
}
}