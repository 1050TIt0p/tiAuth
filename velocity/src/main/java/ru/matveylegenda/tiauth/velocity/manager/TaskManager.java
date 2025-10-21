package ru.matveylegenda.tiauth.velocity.manager;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskManager {
    private final Map<UUID, ScheduledTask> authTimeoutTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> authReminderTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> displayTimerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    private final TiAuth plugin;

    public TaskManager(TiAuth plugin) {
        this.plugin = plugin;
    }

    public void startAuthTimeoutTask(Player player) {
        ScheduledTask task = plugin.getServer().getScheduler().buildTask(plugin, () -> {
            if (!player.isActive()) {
                ScheduledTask old = authTimeoutTasks.remove(player.getUniqueId());
                if (old != null) old.cancel();
                return;
            }
            player.disconnect(CachedComponents.IMP.player.kick.timeout);
        }).delay(MainConfig.IMP.auth.timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS).schedule();

        authTimeoutTasks.put(player.getUniqueId(), task);
    }

    public void startAuthReminderTask(Player player, Component reminder) {
        ScheduledTask task = plugin.getServer().getScheduler().buildTask(plugin, () -> {
            if (!player.isActive()) {
                ScheduledTask old = authReminderTasks.remove(player.getUniqueId());
                if (old != null) old.cancel();
                return;
            }
            player.sendMessage(reminder);
        }).repeat(MainConfig.IMP.auth.reminderInterval, java.util.concurrent.TimeUnit.SECONDS).schedule();

        authReminderTasks.put(player.getUniqueId(), task);
    }

    public void startDisplayTimerTask(Player player) {
        AtomicInteger counter = new AtomicInteger(MainConfig.IMP.auth.timeoutSeconds);
        UUID pid = player.getUniqueId();

        if (MainConfig.IMP.bossBar.enabled) {
            BossBar bar = BossBar.bossBar(
                    Component.empty(),
                    1.0f,
                    BossBar.Color.valueOf(MainConfig.IMP.bossBar.color.getName()),
                    BossBar.Overlay.valueOf(MainConfig.IMP.bossBar.style.getName())
            );
            bossBars.put(pid, bar);
            player.showBossBar(bar);
        }

        ScheduledTask task = plugin.getServer().getScheduler().buildTask(plugin, () -> {
            int c = counter.get();
            if (c <= 0 || !player.isActive()) {
                clearDisplays(player);
                ScheduledTask old = displayTimerTasks.remove(pid);
                if (old != null) old.cancel();
                return;
            }

            if (MainConfig.IMP.title.enabled) {
                sendTitle(player, c);
            }
            if (MainConfig.IMP.actionBar.enabled) {
                sendActionBar(player, c);
            }
            if (MainConfig.IMP.bossBar.enabled) {
                updateBossBar(player, c);
            }

            counter.decrementAndGet();
        }).repeat(1, java.util.concurrent.TimeUnit.SECONDS).schedule();

        displayTimerTasks.put(pid, task);
    }

    private void sendTitle(Player player, int counter) {
        Title componentTitle = Title.title(
                CachedComponents.IMP.player.title.title.replaceText(builder -> builder
                        .match(VelocityUtils.TIME)
                        .replacement(String.valueOf(counter))),
                CachedComponents.IMP.player.title.subTitle.replaceText(builder -> builder
                        .match(VelocityUtils.TIME)
                        .replacement(String.valueOf(counter))),
                0,
                21,
                0);
        player.showTitle(componentTitle);
    }

    private void sendActionBar(Player player, int counter) {
        Component comp = CachedComponents.IMP.player.actionBar.message.replaceText(builder -> builder
                .match(VelocityUtils.TIME)
                .replacement(String.valueOf(counter)));
        player.sendActionBar(comp);
    }

    private void updateBossBar(Player player, int counter) {
        UUID pid = player.getUniqueId();
        BossBar bar = bossBars.get(pid);
        if (bar != null) {
            bar.name(CachedComponents.IMP.player.bossBar.message.replaceText(builder -> builder
                    .match(VelocityUtils.TIME)
                    .replacement(String.valueOf(counter))));
            bar.progress((float) counter / (float) MainConfig.IMP.auth.timeoutSeconds);
        }
    }

    private void clearDisplays(Player player) {
        player.sendActionBar(Component.empty());
        UUID pid = player.getUniqueId();
        BossBar bar = bossBars.remove(pid);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void cancelTasks(Player player) {
        UUID pid = player.getUniqueId();

        ScheduledTask t;

        t = authTimeoutTasks.remove(pid);
        if (t != null) t.cancel();

        t = authReminderTasks.remove(pid);
        if (t != null) t.cancel();

        t = displayTimerTasks.remove(pid);
        if (t != null) t.cancel();

        BossBar bar = bossBars.remove(pid);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }
}
