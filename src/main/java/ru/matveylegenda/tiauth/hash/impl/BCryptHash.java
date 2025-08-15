package ru.matveylegenda.tiauth.hash.impl;

import at.favre.lib.crypto.bcrypt.BCrypt;
import ru.matveylegenda.tiauth.hash.Hash;

public class BCryptHash implements Hash {
    private static final int COST = 12;
    private static final BCrypt.Hasher HASHER = BCrypt.with(BCrypt.Version.VERSION_2A);
    private static final BCrypt.Verifyer VERIFYER = BCrypt.verifyer(BCrypt.Version.VERSION_2A);

    @Override
    public String hashPassword(String password) {
        return HASHER.hashToString(COST, password.toCharArray());
    }

    @Override
    public boolean verifyPassword(String password, String hashedPassword) {
        return VERIFYER.verify(password.toCharArray(), hashedPassword).verified;
    }
}
