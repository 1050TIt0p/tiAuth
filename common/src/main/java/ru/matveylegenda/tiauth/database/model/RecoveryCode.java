package ru.matveylegenda.tiauth.database.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecoveryCode {
    private String recoveryCode;
    private String username;

    public RecoveryCode(String recoveryCode, String username) {
        this.recoveryCode = recoveryCode;
        this.username = username;
    }
}
