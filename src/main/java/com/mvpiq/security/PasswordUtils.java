package com.mvpiq.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // Genera un salt casuale
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    // Hash della password con salt
    public static String hashPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Verifica password
    public static boolean verifyPassword(String password, String storedHash, byte[] salt) {
        String hashOfInput = hashPassword(password, salt);
        return hashOfInput.equals(storedHash);
    }

    // Convenienza: combina salt e hash in una stringa (opzionale)
    public static String hashWithSaltString(String password) {
        byte[] salt = generateSalt();
        String hash = hashPassword(password, salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + hash;
    }

    public static boolean verifyWithSaltString(String password, String stored) {
        if (!stored.contains(":")) {
            return password.equals(stored);
        } else {
            String[] parts = stored.split(":");
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String hash = parts[1];
            return verifyPassword(password, hash, salt);
        }
    }
}