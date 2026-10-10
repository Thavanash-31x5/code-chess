package app.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

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
}