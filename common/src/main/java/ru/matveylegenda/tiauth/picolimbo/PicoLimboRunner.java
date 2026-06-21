package ru.matveylegenda.tiauth.picolimbo;

import com.sun.jna.Pointer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jetbrains.annotations.Nullable;

public class PicoLimboRunner implements Runnable {
    private final int port;
    private final Path configPath;
    private final LibraryLoader.RustLib lib;

    @Nullable
    private volatile Pointer cancellation_token;

    public PicoLimboRunner(int port, Path configPath, LibraryLoader.RustLib lib) {
        this.port = port;
        this.configPath = configPath;
        this.lib = lib;
    }

    @Override
    public void run() {
        extractDefaultFiles();

        String[] args = new String[]{
                "pico_limbo_java_wrapper",
                "--config", configPath.toString(),
                "--port", String.valueOf(port)
        };

        cancellation_token = lib.get_cancellation_token();
        try {
            lib.start_app(cancellation_token, args.length, args);
        } finally {
            lib.cleanup_token(cancellation_token);
            cancellation_token = null;
        }
    }

    public void stop() {
        if (cancellation_token != null) {
            lib.stop_app(cancellation_token);
        }
    }

    private void extractDefaultFiles() {
        Path limboFolder = configPath.getParent();

        try {
            Path schematicPath = limboFolder.resolve("spawn.schem");
            extractResource("/picolimbo/spawn.schem", schematicPath);

            extractResource("/picolimbo/config.toml", configPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractResource(String resourcePath, Path targetPath) throws Exception {
        if (Files.exists(targetPath)) {
            return;
        }

        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println(resourcePath + " = null!!!!!!!!!!!!");
                return;
            }

            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}