package ru.matveylegenda.tiauth.velocity.api.event;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PlayerRegisterEvent {
    private final Player player;
    @Setter
    private boolean moveToBackendServer = true;
    @Setter
    private boolean forceLogin;

    public PlayerRegisterEvent(Player player) {
        this.player = player;
    }

    public PlayerRegisterEvent(Player player, boolean forceLogin) {
        this.player = player;
        this.forceLogin = forceLogin;
    }
}