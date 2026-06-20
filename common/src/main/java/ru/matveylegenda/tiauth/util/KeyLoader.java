package ru.matveylegenda.tiauth.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

public class KeyLoader {

    public static byte[] loadOrGenerateKey(Path dataFolder) throws IOException {
        Path keyPath = dataFolder.resolve("secret.key");

        if (!Files.exists(keyPath)) {
            byte[] newKey = new byte[32];
            new SecureRandom().nextBytes(newKey);

            String encodedKey = Base64.getEncoder().encodeToString(newKey);
            Files.writeString(keyPath, encodedKey);
            return newKey;
        }

        String encodedKey = Files.readString(keyPath);
        return Base64.getDecoder().decode(encodedKey);
    }
}
