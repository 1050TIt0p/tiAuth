package ru.matveylegenda.tiauth.manager;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.protocol.packet.BossBar;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.util.Utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.matveylegenda.tiauth.util.Utils.colorizeComponent;

public class TaskManager {
    private final Map<String, ScheduledTask> authTimeoutTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> authReminderTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> titleTimerTasks = new ConcurrentHashMap<>();
    private final Map<String, UUID> bossBars = new ConcurrentHashMap<>();
    private final TiAuth plugin;
    private final MainConfig mainConfig;
    private final MessagesConfig messagesConfig;
    private final AuthCache authCache;
    private final Utils utils;

    public TaskManager(TiAuth plugin) {
        this.plugin = plugin;
        this.mainConfig = plugin.getMainConfig();
        this.messagesConfig = plugin.getMessagesConfig();
        this.authCache = plugin.getAuthCache();
        this.utils = plugin.getUtils();
    }

    public void startAuthTimeoutTask(ProxiedPlayer player) {
        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected() || authCache.isAuthenticated(player.getName())) {
                authTimeoutTasks.remove(player.getName());
                return;
            }

            utils.kickPlayer(
                    player,
                    messagesConfig.kick.timeout
            );
        }, mainConfig.auth.timeoutSeconds, TimeUnit.SECONDS);

        authTimeoutTasks.put(player.getName(), task);
    }

    public void startAuthReminderTask(ProxiedPlayer player, String reminderMessage) {
        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected() || authCache.isAuthenticated(player.getName())) {
                authReminderTasks.remove(player.getName()).cancel();
                return;
            }

            utils.sendMessage(
                    player,
                    reminderMessage
            );
        }, 0, mainConfig.auth.reminderInterval, TimeUnit.SECONDS);

        authReminderTasks.put(player.getName(), task);
    }

    public void startDisplayTimerTask(ProxiedPlayer player) {
        AtomicInteger counter = new AtomicInteger(mainConfig.auth.timeoutSeconds);

        UUID barId = UUID.randomUUID();
        if (mainConfig.bossBar.enabled) createBossBar(player, counter.get(), barId);

        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (counter.get() <= 0 || !player.isConnected() || authCache.isAuthenticated(player.getName())) {
                clearDisplays(player, barId);
                titleTimerTasks.remove(player.getName()).cancel();
                return;
            }

            if (mainConfig.title.enabled) sendTitle(player, counter.get());
            if (mainConfig.actionBar.enabled) sendActionBar(player, counter.get());
            if (mainConfig.bossBar.enabled) updateBossBar(player, counter.get(), barId);

            counter.getAndDecrement();
        }, 0, 1, TimeUnit.SECONDS);

        titleTimerTasks.put(player.getName(), task);
    }

    private void createBossBar(ProxiedPlayer player, int counter, UUID barId) {
        bossBars.put(player.getName(), barId);

        BossBar bossBar = new BossBar(barId, 0);
        bossBar.setTitle(
                colorizeComponent(
                        messagesConfig.bossBar.message
                                .replace("{prefix}", messagesConfig.prefix)
                                .replace("{time}", String.valueOf(counter))
                )
        );
        bossBar.setHealth(1.0f);
        bossBar.setColor(mainConfig.bossBar.color.getId());
        bossBar.setDivision(mainConfig.bossBar.style.getId());
        bossBar.setFlags((byte)0);
        player.unsafe().sendPacket(bossBar);
    }

    private void updateBossBar(ProxiedPlayer player, int counter, UUID barId) {
        BossBar updateHealth = new BossBar(barId, 2);
        updateHealth.setHealth((float) counter / mainConfig.auth.timeoutSeconds);
        player.unsafe().sendPacket(updateHealth);

        BossBar updateTitle = new BossBar(barId, 3);
        updateTitle.setTitle(colorizeComponent(
                messagesConfig.bossBar.message
                        .replace("{prefix}", messagesConfig.prefix)
                        .replace("{time}", String.valueOf(counter))
        ));
        player.unsafe().sendPacket(updateTitle);
    }

    private void sendTitle(ProxiedPlayer player, int counter) {
        Title title = ProxyServer.getInstance().createTitle();
        title.title(colorizeComponent(
                messagesConfig.title.title
                        .replace("{prefix}", messagesConfig.prefix)
                        .replace("{time}", String.valueOf(counter))
        ));
        title.subTitle(colorizeComponent(
                messagesConfig.title.subTitle
                        .replace("{prefix}", messagesConfig.prefix)
                        .replace("{time}", String.valueOf(counter))
        ));
        title.fadeIn(0);
        title.stay(21);
        title.fadeOut(0);

        player.sendTitle(title);
    }

    private void sendActionBar(ProxiedPlayer player, int counter) {
        player.sendMessage(ChatMessageType.ACTION_BAR, colorizeComponent(
                messagesConfig.actionBar.message
                        .replace("{prefix}", messagesConfig.prefix)
                        .replace("{time}", String.valueOf(counter))
        ));
    }

    private void clearDisplays(ProxiedPlayer player, UUID barId) {
        player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(""));

        BossBar remove = new BossBar(barId, 1);
        player.unsafe().sendPacket(remove);
    }

    public void cancelTasks(ProxiedPlayer player) {
        ScheduledTask task;

        task = authTimeoutTasks.remove(player.getName());
        if (task != null) {
            task.cancel();
        }

        task = authReminderTasks.remove(player.getName());
        if (task != null) {
            task.cancel();
        }

        task = titleTimerTasks.remove(player.getName());
        if (task != null) {
            task.cancel();
        }

        UUID barId = bossBars.remove(player.getName());
        if (barId != null) {
            BossBar remove = new BossBar(barId, 1);
            player.unsafe().sendPacket(remove);
        }
    }
}
