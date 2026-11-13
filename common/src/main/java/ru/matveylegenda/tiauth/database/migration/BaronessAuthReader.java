package ru.matveylegenda.tiauth.database.migration;

import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.migration.BaronessAuthPasswordConverter.Format;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.hash.HashType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class BaronessAuthReader {
    private static final String PROFILES_TABLE = "baronessauth_profiles";
    private static final String LOGINS_TABLE = "baronessauth_logins";
    private static final int NAMES_PER_LOG_LINE = 50;

    private BaronessAuthReader() {
    }

    public static List<AuthUser> read(Statement statement) throws SQLException {
        Map<Long, LoginHistory> histories = readLogins(statement);
        HashType hashType = MainConfig.IMP.auth.hashAlgorithm;

        List<AuthUser> authUsers = new ArrayList<>();
        Map<Format, Integer> formats = new EnumMap<>(Format.class);
        Map<Format, Integer> mismatched = new EnumMap<>(Format.class);
        List<String> unsupported = new ArrayList<>();
        int premiumCount = 0;

        try (ResultSet resultSet = statement.executeQuery("SELECT * FROM " + PROFILES_TABLE)) {
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                String realName = resultSet.getString("name");
                String username = resultSet.getString("name_ci");
                if (username == null || username.isEmpty()) {
                    username = realName.toLowerCase(Locale.ROOT);
                }

                BaronessAuthPasswordConverter.Result password = BaronessAuthPasswordConverter.convert(
                        resultSet.getBytes("password")
                );
                boolean premium = resultSet.getBoolean("premium");
                if (premium) {
                    premiumCount++;
                }

                formats.merge(password.format(), 1, Integer::sum);
                if (!password.supported()) {
                    if (!(password.format() == Format.EMPTY && premium)) {
                        unsupported.add(realName);
                    }
                } else if (!password.format().isVerifiableBy(hashType)) {
                    mismatched.merge(password.format(), 1, Integer::sum);
                }

                LoginHistory history = histories.get(id);
                long now = System.currentTimeMillis();

                authUsers.add(
                        new AuthUser(
                                username,
                                realName,
                                password.password(),
                                premium,
                                history != null ? history.lastIp : null,
                                history != null ? history.firstIp : null,
                                history != null ? history.lastDate : now,
                                history != null ? history.firstDate : now
                        )
                );
            }
        }

        Database.LOGGER.info("BaronessAuth profiles read: " + authUsers.size() + ", password formats: " + formats);
        if (!mismatched.isEmpty()) {
            Database.LOGGER.warning("BaronessAuth: passwords of these formats cannot be verified with the current"
                    + " hash-algorithm " + hashType + ", their owners will not be able to log in until it is changed: "
                    + mismatched);
        }
        if (premiumCount > 0) {
            Database.LOGGER.warning("BaronessAuth: " + premiumCount + " premium profiles migrated. tiAuth gives premium"
                    + " players offline-mode UUIDs, while BaronessAuth keeps their Mojang UUIDs unless"
                    + " force-set-player-uuid is enabled, so data bound to UUIDs on backend servers may not carry over.");
        }
        if (!unsupported.isEmpty()) {
            Database.LOGGER.warning("BaronessAuth: " + unsupported.size()
                    + " profiles have no password or a password in a format tiAuth cannot verify"
                    + " (MD5, salted SHA-512, legacy composite, bcrypt v3, Argon2i/Argon2d)."
                    + " They were migrated, but an administrator has to set new passwords for them:");
            for (int i = 0; i < unsupported.size(); i += NAMES_PER_LOG_LINE) {
                Database.LOGGER.warning(String.join(
                        ", ",
                        unsupported.subList(i, Math.min(i + NAMES_PER_LOG_LINE, unsupported.size()))
                ));
            }
        }

        return authUsers;
    }

    private static Map<Long, LoginHistory> readLogins(Statement statement) throws SQLException {
        String date = quote(statement, "date");
        String boundaryLogins = "SELECT l.profile_id, l.address, l." + date + " FROM " + LOGINS_TABLE + " l JOIN ("
                + "SELECT profile_id, MIN(" + date + ") AS first_date, MAX(" + date + ") AS last_date"
                + " FROM " + LOGINS_TABLE + " GROUP BY profile_id"
                + ") b ON b.profile_id = l.profile_id AND (l." + date + " = b.first_date OR l." + date + " = b.last_date)";

        try {
            return collectLogins(statement, boundaryLogins);
        } catch (SQLException e) {
            Database.LOGGER.log(Level.WARNING, "BaronessAuth: failed to aggregate the login history, reading it in full", e);
            return collectLogins(statement, "SELECT * FROM " + LOGINS_TABLE);
        }
    }

    private static Map<Long, LoginHistory> collectLogins(Statement statement, String query) throws SQLException {
        Map<Long, LoginHistory> histories = new HashMap<>();

        try (ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp("date");
                if (timestamp == null) {
                    continue;
                }

                long profileId = resultSet.getLong("profile_id");
                long date = timestamp.getTime();
                int rawAddress = resultSet.getInt("address");
                String address = resultSet.wasNull() ? null : toIpv4(rawAddress);

                histories.computeIfAbsent(profileId, key -> new LoginHistory()).add(date, address);
            }
        }

        return histories;
    }

    private static String quote(Statement statement, String identifier) throws SQLException {
        String quoteString = statement.getConnection().getMetaData().getIdentifierQuoteString();
        if (quoteString == null || quoteString.isBlank()) {
            return identifier;
        }
        return quoteString + identifier + quoteString;
    }

    private static String toIpv4(int address) {
        return ((address >>> 24) & 0xFF) + "." + ((address >>> 16) & 0xFF) + "." + ((address >>> 8) & 0xFF) + "." + (address & 0xFF);
    }

    private static final class LoginHistory {
        private long firstDate = Long.MAX_VALUE;
        private String firstIp;
        private long lastDate = Long.MIN_VALUE;
        private String lastIp;

        private void add(long date, String address) {
            if (date < firstDate) {
                firstDate = date;
                firstIp = address;
            }
            if (date >= lastDate) {
                lastDate = date;
                lastIp = address;
            }
        }
    }
}
