package ru.matveylegenda.tiauth.hash.impl;

import ru.matveylegenda.tiauth.hash.Hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Sha256Hash implements Hash {

    @Override
    public String hashPassword(String password) {
        return sha256(password);
    }

    @Override
    public boolean verifyPassword(String password, String hashedPassword) {
        return hashedPassword.equals(sha256(password));
    }

    private String sha256(String message) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHexString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
