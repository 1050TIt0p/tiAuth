package ru.matveylegenda.tiauth.database.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.UUID;

@DatabaseTable(tableName = "premium_identities")
@Data
@NoArgsConstructor
public class PremiumIdentity {
    @DatabaseField(id = true, canBeNull = false)
    private String username;

    @DatabaseField(canBeNull = false)
    private String uuid;

    public PremiumIdentity(String username, UUID uuid) {
        this.username = username.toLowerCase(Locale.ROOT);
        this.uuid = uuid.toString();
    }
}
