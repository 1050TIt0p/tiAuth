package ru.matveylegenda.tiauth.bungee.api.event;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

@Getter
public class PlayerAuthEvent extends Event {
    private final ProxiedPlayer player;
    @Setter
    private boolean moveToBackendServer = true;

    public PlayerAuthEvent(ProxiedPlayer player) {
        this.player = player;
    }
}