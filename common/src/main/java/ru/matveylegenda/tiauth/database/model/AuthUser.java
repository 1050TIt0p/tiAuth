package ru.matveylegenda.tiauth.database.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthUser {
    private String username;
    private String realName;
    private String password;
    private boolean premium;
    private String lastIp;
    private String regIp;
    private long lastLogin;
    private long regDate;
    private String totpToken = "";

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

    public AuthUser(String username, String realName, String password, boolean premium, String lastIp, String regIp, long lastLogin, long regDate) {
        this.username = username;
        this.realName = realName;
        this.password = password;
        this.premium = premium;
        this.lastIp = lastIp;
        this.regIp = regIp;
        this.lastLogin = lastLogin;
        this.regDate = regDate;
    }
}
