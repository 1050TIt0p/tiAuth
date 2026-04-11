package ru.matveylegenda.tiauth.bukkit.api.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class PlayerAuthEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    @Setter
    private boolean moveToBackendServer = true;

    public PlayerAuthEvent(Player player) {
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
