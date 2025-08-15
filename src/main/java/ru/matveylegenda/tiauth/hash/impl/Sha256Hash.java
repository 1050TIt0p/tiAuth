package ru.matveylegenda.tiauth.hash.impl;

import ru.matveylegenda.tiauth.hash.Hash;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Sha256Hash implements Hash {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String hashPassword(String password) {
        String salt = generateSalt(16);
        String hash = sha256(sha256(password) + salt);
        return "$SHA$" + salt + "$" + hash;
    }

    @Override
    public boolean verifyPassword(String password, String hashedPassword) {
        String[] parts = hashedPassword.split("\\$");
        if (parts.length != 4) return false;
        String salt = parts[2];
        String hash = parts[3];
        return isEqual(hash, sha256(sha256(password) + salt));
    }

    private String generateSalt(int length) {
        byte[] saltBytes = new byte[length];
        RANDOM.nextBytes(saltBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : saltBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String sha256(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(message.getBytes(StandardCharsets.UTF_8));
            return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEqual(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
