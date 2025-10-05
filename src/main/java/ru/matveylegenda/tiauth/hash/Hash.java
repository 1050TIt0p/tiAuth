package ru.matveylegenda.tiauth.hash;

public interface Hash {
    String hashPassword(String password);

    boolean verifyPassword(String password, String hashedPassword);
}
