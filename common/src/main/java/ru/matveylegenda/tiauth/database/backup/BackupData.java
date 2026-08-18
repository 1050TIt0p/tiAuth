package ru.matveylegenda.tiauth.database.backup;

import com.google.gson.JsonElement;
import ru.matveylegenda.tiauth.database.model.AuthUser;
import ru.matveylegenda.tiauth.database.model.RecoveryCode;

import java.util.List;
import java.util.Map;

record BackupData(
        int version,
        List<AuthUser> users,
        List<RecoveryCode> recoveryCodes,
        Map<String, JsonElement> addons
) {
    static final int CURRENT_VERSION = 1;

    boolean isValid() {
        return version == CURRENT_VERSION && users != null && recoveryCodes != null && addons != null;
    }
}
