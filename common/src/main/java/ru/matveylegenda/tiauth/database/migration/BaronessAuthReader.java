package ru.matveylegenda.tiauth.database.migration;

import ru.matveylegenda.tiauth.database.Database;
import ru.matveylegenda.tiauth.database.model.AuthUser;

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

public final class BaronessAuthReader {
    private static final String PROFILES_TABLE = "baronessauth_profiles";
    private static final String LOGINS_TABLE = "baronessauth_logins";

    private BaronessAuthReader() {
    }

    public static List<AuthUser> read(Statement statement) throws SQLException {
        Map<Long, LoginHistory> histories = readLogins(statement);

        List<AuthUser> authUsers = new ArrayList<>();
        Map<BaronessAuthPasswordConverter.Format, Integer> formats = new EnumMap<>(BaronessAuthPasswordConverter.Format.class);
        List<String> unsupported = new ArrayList<>();

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
                formats.merge(password.format(), 1, Integer::sum);
                if (!password.supported() && !(password.format() == BaronessAuthPasswordConverter.Format.EMPTY && premium)) {
                    unsupported.add(realName);
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
        if (!unsupported.isEmpty()) {
            Database.LOGGER.warning("BaronessAuth: " + unsupported.size()
                    + " profiles have no password or a password in a format tiAuth cannot verify"
                    + " (MD5, salted SHA-512, legacy composite or bcrypt v3)."
                    + " They were migrated, but an administrator has to set new passwords for them: "
                    + String.join(", ", unsupported));
        }

        return authUsers;
    }

    private static Map<Long, LoginHistory> readLogins(Statement statement) throws SQLException {
        Map<Long, LoginHistory> histories = new HashMap<>();

        try (ResultSet resultSet = statement.executeQuery("SELECT * FROM " + LOGINS_TABLE)) {
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
