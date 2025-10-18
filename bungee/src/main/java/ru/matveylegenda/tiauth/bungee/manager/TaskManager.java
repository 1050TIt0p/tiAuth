package ru.matveylegenda.tiauth.bungee.manager;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.protocol.packet.BossBar;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskManager {
    private final Map<String, ScheduledTask> authTimeoutTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> authReminderTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> displayTimerTasks = new ConcurrentHashMap<>();
    private final Map<String, UUID> bossBars = new ConcurrentHashMap<>();
    private final TiAuth plugin;

    public TaskManager(TiAuth plugin) {
        this.plugin = plugin;
    }

    public void startAuthTimeoutTask(ProxiedPlayer player) {
        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected()) {
                ScheduledTask task1 = authTimeoutTasks.remove(player.getName());
                if (task1 != null) {
                    task1.cancel();
                }
                return;
            }

            player.disconnect(CachedMessages.IMP.player.kick.timeout);

        }, MainConfig.IMP.auth.timeoutSeconds, TimeUnit.SECONDS);

        authTimeoutTasks.put(player.getName(), task);
    }

    public void startAuthReminderTask(ProxiedPlayer player, String reminderMessage) {
        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected()) {
                ScheduledTask task1 = authReminderTasks.remove(player.getName());
                if (task1 != null) {
                    task1.cancel();
                }
                return;
            }

            BungeeUtils.sendMessage(
                    player,
                    reminderMessage
            );
        }, 0, MainConfig.IMP.auth.reminderInterval, TimeUnit.SECONDS);

        authReminderTasks.put(player.getName(), task);
    }

    public void startDisplayTimerTask(ProxiedPlayer player) {
        AtomicInteger counter = new AtomicInteger(MainConfig.IMP.auth.timeoutSeconds);

        UUID barId = MainConfig.IMP.bossBar.enabled ? UUID.randomUUID() : null;
        if (MainConfig.IMP.bossBar.enabled) createBossBar(player, counter.get(), barId);

        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (counter.get() <= 0 || !player.isConnected()) {
                clearDisplays(player, barId);
                ScheduledTask task1 = displayTimerTasks.remove(player.getName());
                if (task1 != null) {
                    task1.cancel();
                }
                return;
            }

            if (MainConfig.IMP.title.enabled) sendTitle(player, counter.get());
            if (MainConfig.IMP.actionBar.enabled) sendActionBar(player, counter.get());
            if (MainConfig.IMP.bossBar.enabled) updateBossBar(player, counter.get(), barId);

            counter.getAndDecrement();
        }, 0, 1, TimeUnit.SECONDS);

        displayTimerTasks.put(player.getName(), task);
    }

    private void createBossBar(ProxiedPlayer player, int counter, UUID barId) {
        bossBars.put(player.getName(), barId);

        BossBar bossBar = new BossBar(barId, 0);
        bossBar.setTitle(
                TextComponent.fromLegacy(
                        CachedMessages.IMP.player.bossBar.message
                                .replace("{time}", String.valueOf(counter))
                )
        );
        bossBar.setHealth(1.0f);
        bossBar.setColor(MainConfig.IMP.bossBar.color.getId());
        bossBar.setDivision(MainConfig.IMP.bossBar.style.getId());
        bossBar.setFlags((byte) 0);
        player.unsafe().sendPacket(bossBar);
    }

    private void updateBossBar(ProxiedPlayer player, int counter, UUID barId) {
        BossBar updateHealth = new BossBar(barId, 2);
        updateHealth.setHealth((float) counter / MainConfig.IMP.auth.timeoutSeconds);
        player.unsafe().sendPacket(updateHealth);

        BossBar updateTitle = new BossBar(barId, 3);
        updateTitle.setTitle(TextComponent.fromLegacy(
                CachedMessages.IMP.player.bossBar.message
                        .replace("{time}", String.valueOf(counter))
        ));
        player.unsafe().sendPacket(updateTitle);
    }

    private void sendTitle(ProxiedPlayer player, int counter) {
        Title title = ProxyServer.getInstance().createTitle();
        title.title(TextComponent.fromLegacy(
                CachedMessages.IMP.player.title.title
                        .replace("{time}", String.valueOf(counter))
        ));
        title.subTitle(TextComponent.fromLegacy(
                CachedMessages.IMP.player.title.subTitle
                        .replace("{time}", String.valueOf(counter))
        ));
        title.fadeIn(0);
        title.stay(21);
        title.fadeOut(0);

        player.sendTitle(title);
    }

    private void sendActionBar(ProxiedPlayer player, int counter) {
        player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(
                CachedMessages.IMP.player.actionBar.message
                        .replace("{time}", String.valueOf(counter))
        ));
    }

    private void clearDisplays(ProxiedPlayer player, UUID barId) {
        player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(""));

        if (barId != null) {
            BossBar remove = new BossBar(barId, 1);
            player.unsafe().sendPacket(remove);
        }
    }

    public void cancelTasks(ProxiedPlayer player) {
        String playerName = player.getName();
        ScheduledTask task;

        task = authTimeoutTasks.remove(playerName);
        if (task != null) {
            task.cancel();
        }

        task = authReminderTasks.remove(playerName);
        if (task != null) {
            task.cancel();
        }

        task = displayTimerTasks.remove(playerName);
        if (task != null) {
            task.cancel();
        }

        UUID barId = bossBars.remove(playerName);
        if (barId != null) {
            BossBar remove = new BossBar(barId, 1);
            player.unsafe().sendPacket(remove);
        }
    }
}
