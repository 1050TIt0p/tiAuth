package ru.matveylegenda.tiauth.database.backup;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

final class BackupStorage {
    private static final Gson GSON = new Gson();

    void write(Path output, BackupData data, int compressionLevel) {
        Path absoluteOutput = output.toAbsolutePath();
        Path directory = absoluteOutput.getParent();
        Path temporary = null;

        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, absoluteOutput.getFileName().toString(), ".tmp");
            writeCompressed(temporary, data, compressionLevel);
            moveIntoPlace(temporary, absoluteOutput);
        } catch (Exception exception) {
            deleteTemporaryFile(temporary, exception);
            throw new IllegalStateException("Could not write backup file", exception);
        }
    }

    BackupData read(Path input) {
        try (InflaterInputStream compressed = new InflaterInputStream(Files.newInputStream(input));
             BufferedReader reader = new BufferedReader(new InputStreamReader(compressed, StandardCharsets.UTF_8))) {
            BackupData data = GSON.fromJson(reader, BackupData.class);
            if (data == null || !data.isValid()) {
                throw new IllegalArgumentException("Invalid or unsupported backup file");
            }
            return data;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not read backup file", exception);
        }
    }

    static JsonElement toJson(Object data) {
        return GSON.toJsonTree(data);
    }

    static <T> T fromJson(JsonElement json, Class<T> dataType) {
        return GSON.fromJson(json, dataType);
    }

    private void writeCompressed(Path output, BackupData data, int compressionLevel) throws IOException {
        Deflater deflater = new Deflater(compressionLevel);
        try (DeflaterOutputStream compressed = new DeflaterOutputStream(Files.newOutputStream(output), deflater);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(compressed, StandardCharsets.UTF_8))) {
            GSON.toJson(data, writer);
        } finally {
            deflater.end();
        }
    }

    private void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteTemporaryFile(Path temporary, Exception originalException) {
        if (temporary == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporary);
        } catch (IOException exception) {
            originalException.addSuppressed(exception);
        }
    }
}
