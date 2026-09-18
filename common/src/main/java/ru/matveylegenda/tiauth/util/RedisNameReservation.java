package ru.matveylegenda.tiauth.util;

import ru.matveylegenda.tiauth.config.MainConfig;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class RedisNameReservation {
    private RedisNameReservation() {
    }

    public static CompletableFuture<Boolean> isReserved(String username) {
        MainConfig.ReservedAiNames config = MainConfig.IMP.reservedAiNames;
        if (!config.enabled) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query(config, username.toLowerCase(Locale.ROOT));
            } catch (IOException exception) {
                return config.failClosed;
            }
        });
    }

    private static boolean query(MainConfig.ReservedAiNames config, String username) throws IOException {
        Socket socket = config.redisUseSsl
                ? SSLSocketFactory.getDefault().createSocket()
                : new Socket();
        try (socket) {
            int timeout = Math.max(250, Math.min(10000, config.timeoutMillis));
            socket.connect(new InetSocketAddress(config.redisHost, config.redisPort), timeout);
            socket.setSoTimeout(timeout);
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
            if (config.redisPassword != null && !config.redisPassword.isBlank()) {
                command(input, output, "AUTH", config.redisPassword);
            }
            if (config.redisDatabase > 0) {
                command(input, output, "SELECT", String.valueOf(config.redisDatabase));
            }
            return command(input, output, "EXISTS", config.keyPrefix + username).equals("1");
        }
    }

    private static String command(BufferedInputStream input, BufferedOutputStream output, String... values)
            throws IOException {
        output.write(("*" + values.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String value : values) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        output.flush();
        int prefix = input.read();
        String line = readLine(input);
        if (prefix == '-') {
            throw new IOException("Redis rejected reservation lookup");
        }
        return line;
    }

    private static String readLine(BufferedInputStream input) throws IOException {
        StringBuilder result = new StringBuilder();
        while (true) {
            int value = input.read();
            if (value < 0) {
                throw new IOException("Redis closed the connection");
            }
            if (value == '\r') {
                if (input.read() != '\n') {
                    throw new IOException("Invalid Redis response");
                }
                return result.toString();
            }
            result.append((char) value);
        }
    }
}
