package ru.matveylegenda.tiauth.picolimbo;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LibraryLoader {
    public static final Logger LOGGER = Logger.getLogger("tiAuth-PicoLimbo");

    private static final String BASE_URL = "https://github.com/1050TIt0p/PicoLimbo/releases/latest/download/";
    private static final String LIB_NAME_BASE = "pico_limbo_lib";

    public interface RustLib extends Library {
        void start_app(Pointer ptr, int argc, String[] argv);
        void stop_app(Pointer ptr);
        Pointer get_cancellation_token();
        void cleanup_token(Pointer ptr);
    }

    public static RustLib loadOrDownloadLib(Path dataFolder) throws Exception {
        String fileName = getFileName();
        Path libPath = dataFolder.resolve(fileName);
        boolean needsDownload = true;

        if (Files.exists(libPath)) {
            if (MainConfig.IMP.servers.virtualServerAutoUpdate) {
                LOGGER.info("Checking for updates for PicoLimbo...");
                String localHash = calculateLocalFileHash(libPath);
                String remoteHash = getRemoteHash(fileName);

                if (remoteHash == null) {
                    LOGGER.info("Update check failed");
                    needsDownload = false;
                } else if (remoteHash.equalsIgnoreCase(localHash)) {
                    LOGGER.info("You are using the latest version of PicoLimbo");
                    needsDownload = false;
                } else {
                    LOGGER.info("A new version of PicoLimbo is available. Updating...");
                }
            } else {
                needsDownload = false;
            }
        }

        if (needsDownload) {
            LOGGER.info("Downloading PicoLimbo...");
            String downloadUrl = BASE_URL + fileName;

            try (InputStream in = URI.create(downloadUrl).toURL().openStream()) {
                Files.copy(in, libPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Download complete");
            } catch (Exception e) {
                throw new RuntimeException("Failed to download PicoLimbo library from: " + downloadUrl, e);
            }
        }

        return Native.load(libPath.toAbsolutePath().toString(), RustLib.class);
    }

    private static String getRemoteHash(String fileName) {
        String hashUrl = BASE_URL + fileName + ".sha256";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                URI.create(hashUrl).toURL().openStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim().split("\\s+")[0];
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not fetch remote hash", e);
        }
        return null;
    }

    private static String calculateLocalFileHash(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String getFileName() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.equals("amd64")) {
            arch = "x86_64";
        }

        if (Platform.isWindows()) {
            return LIB_NAME_BASE + "-windows-x86_64.dll";
        } else if (Platform.isMac()) {
            if (!arch.equals("aarch64")) {
                throw new UnsupportedOperationException("Unsupported macOS arch: " + arch);
            }
            return "lib" + LIB_NAME_BASE + "-macos-aarch64.dylib";
        } else if (Platform.isLinux()) {
            if (arch.equals("x86_64")) {
                return "lib" + LIB_NAME_BASE + "-linux-x86_64-gnu.so";
            } else if (arch.equals("aarch64")) {
                return "lib" + LIB_NAME_BASE + "-linux-aarch64-gnu.so";
            } else {
                throw new UnsupportedOperationException("Unsupported Linux arch: " + arch);
            }
        }
        throw new UnsupportedOperationException("Unsupported OS: " + os);
    }
}
