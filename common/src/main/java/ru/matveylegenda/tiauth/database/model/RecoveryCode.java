package ru.matveylegenda.tiauth.database.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.NoArgsConstructor;

@DatabaseTable(tableName = "recovery_codes")
@Data
@NoArgsConstructor
public class RecoveryCode {
    @DatabaseField(id = true, canBeNull = false)
    private String recoveryCode;

    @DatabaseField(canBeNull = false)
    private String username;

    public RecoveryCode(String recoveryCode, String username) {
        this.recoveryCode = recoveryCode;
        this.username = username;
    }
}
