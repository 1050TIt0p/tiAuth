package ru.matveylegenda.tiauth.hash.impl;

import ru.matveylegenda.tiauth.hash.Hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Sha256Hash implements Hash {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final MessageDigest MESSAGE_DIGEST;

    static {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        MESSAGE_DIGEST = md;
    }

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
        return hash.equals(sha256(sha256(password) + salt));
    }

    private String generateSalt(int length) {
        byte[] saltBytes = new byte[length];
        RANDOM.nextBytes(saltBytes);
        return bytesToHexString(saltBytes);
    }

    private String sha256(String message) {
        byte[] digest = MESSAGE_DIGEST.digest(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHexString(digest);
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
