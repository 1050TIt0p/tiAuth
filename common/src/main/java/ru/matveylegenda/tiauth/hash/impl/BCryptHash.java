package ru.matveylegenda.tiauth.hash.impl;

import at.favre.lib.crypto.bcrypt.BCrypt;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.hash.Hash;

public class BCryptHash implements Hash {
    private static final BCrypt.Hasher HASHER = BCrypt.withDefaults();
    private static final BCrypt.Verifyer VERIFYER = BCrypt.verifyer();

    @Override
    public String hashPassword(String password) {
        return HASHER.hashToString(MainConfig.IMP.auth.bcryptCost, password.toCharArray());
    }

    @Override
    public boolean verifyPassword(String password, String hashedPassword) {
        return VERIFYER.verify(password.toCharArray(), hashedPassword).verified;
    }
}
