package ru.matveylegenda.tiauth.velocity.api.event;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PlayerAuthEvent {
    private final Player player;
    @Setter
    private boolean moveToBackendServer = true;
    @Setter
    private boolean forceLogin;

    public PlayerAuthEvent(Player player) {
        this.player = player;
    }

    public PlayerAuthEvent(Player player, boolean forceLogin) {
        this.player = player;
        this.forceLogin = forceLogin;
    }
}