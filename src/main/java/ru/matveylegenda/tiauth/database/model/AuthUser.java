package ru.matveylegenda.tiauth.database.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.NoArgsConstructor;

@DatabaseTable(tableName = "auth_users")
@Data
@NoArgsConstructor
public class AuthUser {
    @DatabaseField(id = true, canBeNull = false)
    private String username;

    @DatabaseField(canBeNull = false)
    private String realName;

    @DatabaseField(canBeNull = false)
    private String password;

    @DatabaseField
    private boolean premium;

    @DatabaseField
    private String lastIp;

    @DatabaseField
    private String regIp;

    @DatabaseField
    private long lastLogin;

    @DatabaseField
    private long regDate;

    public AuthUser(String username, String realName, String password, boolean premium, String regIp) {
        this.username = username;
        this.realName = realName;
        this.password = password;
        this.premium = premium;
        this.regIp = regIp;
        this.lastIp = regIp;
        long now = System.currentTimeMillis();
        this.regDate = now;
        this.lastLogin = now;
    }

    public AuthUser(String username, String realName, String password, boolean premium, String regIp, long regDate, long lastLogin) {
        this.username = username;
        this.realName = realName;
        this.password = password;
        this.premium = premium;
        this.regIp = regIp;
        this.lastIp = regIp;
        this.regDate = regDate;
        this.lastLogin = lastLogin;
    }
}