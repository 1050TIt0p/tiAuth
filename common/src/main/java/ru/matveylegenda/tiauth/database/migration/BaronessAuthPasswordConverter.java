package ru.matveylegenda.tiauth.database.migration;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.BCryptFormatter;
import at.favre.lib.crypto.bcrypt.Radix64Encoder;
import ru.matveylegenda.tiauth.hash.HashType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

public final class BaronessAuthPasswordConverter {
    private static final String UNSUPPORTED_PREFIX = "baronessauth:";

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[abxy]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    private static final Pattern HEX_SHA256_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final BCryptFormatter BCRYPT_FORMATTER = new BCryptFormatter.Default(
            new Radix64Encoder.Default(),
            StandardCharsets.UTF_8
    );

    private static final int MAX_PASSWORD_LENGTH = 255;
    private static final int LEGACY_HEADER_LENGTH = 8;
    private static final int SHORT_HEADER_LENGTH = 2;
    private static final int RAW_BCRYPT_SALT_LENGTH = 16;
    private static final int RAW_BCRYPT_HASH_LENGTH = 24;
    private static final int BCRYPT_ENCODED_HASH_LENGTH = 23;

    private BaronessAuthPasswordConverter() {
    }

    public static Result convert(byte[] data) {
        if (data == null || data.length == 0) {
            return new Result(Format.EMPTY, UNSUPPORTED_PREFIX + "empty", false);
        }

        Format format;
        int offset;
        if (data.length >= SHORT_HEADER_LENGTH && data[0] == 0 && data[1] == Format.INTERNAL_BCRYPT_V5.id) {
            format = Format.INTERNAL_BCRYPT_V5;
            offset = SHORT_HEADER_LENGTH;
        } else if (data.length >= LEGACY_HEADER_LENGTH) {
            long id = 0;
            for (int i = 0; i < LEGACY_HEADER_LENGTH; i++) {
                id = (id << 8) | (data[i] & 0xFF);
            }
            format = Format.byLegacyId(id);
            offset = LEGACY_HEADER_LENGTH;
        } else {
            format = Format.UNKNOWN;
            offset = 0;
        }

        byte[] payload = Arrays.copyOfRange(data, offset, data.length);
        String converted = switch (format) {
            case GENERIC_BCRYPT, INTERNAL_BCRYPT_V1, INTERNAL_BCRYPT_V2 -> convertBcryptString(payload);
            case INTERNAL_BCRYPT_V4, INTERNAL_BCRYPT_V5 -> convertRawBcrypt(payload);
            case PLAIN_SHA256 -> convertPlainSha256(payload);
            case GENERIC_SALTED_SHA256 -> convertSaltedSha256(payload);
            case GENERIC_ARGON2 -> convertArgon2(payload);
            default -> null;
        };

        if (converted == null || converted.length() > MAX_PASSWORD_LENGTH) {
            String marker = UNSUPPORTED_PREFIX + format.name().toLowerCase(Locale.ROOT);
            String withData = marker + ":" + Base64.getEncoder().encodeToString(data);
            return new Result(format, withData.length() <= MAX_PASSWORD_LENGTH ? withData : marker, false);
        }

        return new Result(format, converted);
    }

    private static String convertBcryptString(byte[] payload) {
        String hash = new String(payload, StandardCharsets.UTF_8);
        return BCRYPT_PATTERN.matcher(hash).matches() ? hash : null;
    }

    private static String convertRawBcrypt(byte[] payload) {
        if (payload.length != 1 + RAW_BCRYPT_SALT_LENGTH + RAW_BCRYPT_HASH_LENGTH) {
            return null;
        }

        int cost = payload[0] & 0xFF;
        if (cost < 4 || cost > 31) {
            return null;
        }

        byte[] salt = Arrays.copyOfRange(payload, 1, 1 + RAW_BCRYPT_SALT_LENGTH);
        byte[] hash = Arrays.copyOfRange(
                payload,
                1 + RAW_BCRYPT_SALT_LENGTH,
                1 + RAW_BCRYPT_SALT_LENGTH + BCRYPT_ENCODED_HASH_LENGTH
        );

        BCrypt.HashData hashData = new BCrypt.HashData(cost, BCrypt.Version.VERSION_2A, salt, hash);
        return new String(BCRYPT_FORMATTER.createHashMessage(hashData), StandardCharsets.UTF_8);
    }

    private static String convertPlainSha256(byte[] payload) {
        String hash = new String(payload, StandardCharsets.UTF_8);
        return HEX_SHA256_PATTERN.matcher(hash).matches() ? hash.toLowerCase(Locale.ROOT) : null;
    }

    private static String convertSaltedSha256(byte[] payload) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            String hash = input.readUTF();
            String salt = input.readUTF();
            if (!HEX_SHA256_PATTERN.matcher(hash).matches() || salt.isEmpty() || salt.contains("$")) {
                return null;
            }
            return "$SHA$" + salt + "$" + hash.toLowerCase(Locale.ROOT);
        } catch (IOException e) {
            return null;
        }
    }

    private static String convertArgon2(byte[] payload) {
        String hash = new String(payload, StandardCharsets.UTF_8);
        return hash.startsWith("$argon2id$") ? hash : null;
    }

    public record Result(Format format, String password, boolean supported) {
        public Result(Format format, String password) {
            this(format, password, true);
        }
    }

    public enum Format {
        GENERIC_BCRYPT(0),
        INTERNAL_BCRYPT_V1(1),
        INTERNAL_BCRYPT_V2(2),
        PLAIN_MD5(3),
        PLAIN_SHA256(4),
        GENERIC_ARGON2(5),
        LEGACY_COMPOSITE(6),
        GENERIC_SALTED_SHA512(7),
        GENERIC_SALTED_SHA256(8),
        INTERNAL_BCRYPT_V3(9),
        INTERNAL_BCRYPT_V4(10),
        INTERNAL_BCRYPT_V5(11),
        EMPTY(-1),
        UNKNOWN(-1);

        private final int id;

        Format(int id) {
            this.id = id;
        }

        public boolean isVerifiableBy(HashType hashType) {
            return switch (this) {
                case GENERIC_BCRYPT, INTERNAL_BCRYPT_V1, INTERNAL_BCRYPT_V2, INTERNAL_BCRYPT_V4, INTERNAL_BCRYPT_V5 ->
                        hashType == HashType.BCRYPT;
                case PLAIN_SHA256 -> hashType == HashType.SHA256 || hashType == HashType.SHA256_DEFAULT;
                case GENERIC_SALTED_SHA256 -> hashType == HashType.SHA256;
                case GENERIC_ARGON2 -> hashType == HashType.ARGON2;
                default -> false;
            };
        }

        private static Format byLegacyId(long id) {
            for (Format format : values()) {
                if (format.id == id && format != INTERNAL_BCRYPT_V5) {
                    return format;
                }
            }
            return UNKNOWN;
        }
    }
}
