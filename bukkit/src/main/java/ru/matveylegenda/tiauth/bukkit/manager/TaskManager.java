package ru.matveylegenda.tiauth.bukkit.manager;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.matveylegenda.tiauth.bukkit.TiAuth;
import ru.matveylegenda.tiauth.bukkit.storage.CachedMessages;
import ru.matveylegenda.tiauth.bukkit.util.BukkitUtils;
import ru.matveylegenda.tiauth.config.MainConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private final Map<String, BukkitTask> authTimeoutTasks = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> authReminderTasks = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> displayTimerTasks = new ConcurrentHashMap<>();
    private final Map<String, BossBar> bossBars = new ConcurrentHashMap<>();
    private final TiAuth plugin;

    public TaskManager(TiAuth plugin) {
        this.plugin = plugin;
    }

    public void startAuthTimeoutTask(Player player) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                authTimeoutTasks.remove(player.getName());
                return;
            }

            player.kickPlayer(BukkitUtils.colorize(CachedMessages.IMP.player.kick.timeout));
        }, MainConfig.IMP.auth.timeoutSeconds * 20L);

        authTimeoutTasks.put(player.getName(), task);
    }

    public void startAuthReminderTask(Player player, String reminderMessage) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                BukkitTask task1 = authReminderTasks.remove(player.getName());
                if (task1 != null) {
                    task1.cancel();
                }
                return;
            }

            BukkitUtils.sendMessage(player, reminderMessage);
        }, 0, MainConfig.IMP.auth.reminderInterval * 20L);

        authReminderTasks.put(player.getName(), task);
    }

    public void startDisplayTimerTask(Player player) {
        int timeoutSeconds = MainConfig.IMP.auth.timeoutSeconds;
        final int[] counter = {timeoutSeconds};

        BossBar bossBar = null;
        if (MainConfig.IMP.bossBar.enabled) {
            bossBar = Bukkit.createBossBar(
                    BukkitUtils.colorize(CachedMessages.IMP.player.bossBar.message.replace("{time}", String.valueOf(counter[0]))),
                    BarColor.valueOf(MainConfig.IMP.bossBar.color.name()),
                    BarStyle.valueOf(MainConfig.IMP.bossBar.style.name())
            );
            bossBar.addPlayer(player);
            bossBars.put(player.getName(), bossBar);
        }

        BossBar finalBossBar = bossBar;
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (counter[0] <= 0 || !player.isOnline()) {
                clearDisplays(player);
                BukkitTask task1 = displayTimerTasks.remove(player.getName());
                if (task1 != null) {
                    task1.cancel();
                }
                return;
            }

            if (MainConfig.IMP.title.enabled) {
                player.sendTitle(
                        BukkitUtils.colorize(CachedMessages.IMP.player.title.title.replace("{time}", String.valueOf(counter[0]))),
                        BukkitUtils.colorize(CachedMessages.IMP.player.title.subTitle.replace("{time}", String.valueOf(counter[0]))),
                        0, 20, 0
                );
            }

            if (MainConfig.IMP.actionBar.enabled) {
                BukkitUtils.sendActionBar(player, CachedMessages.IMP.player.actionBar.message.replace("{time}", String.valueOf(counter[0])));
            }

            if (finalBossBar != null) {
                finalBossBar.setTitle(BukkitUtils.colorize(CachedMessages.IMP.player.bossBar.message.replace("{time}", String.valueOf(counter[0]))));
                finalBossBar.setProgress((double) counter[0] / timeoutSeconds);
            }

            counter[0]--;
        }, 0, 20L);

        displayTimerTasks.put(player.getName(), task);
    }

    public void clearDisplays(Player player) {
        BossBar bossBar = bossBars.remove(player.getName());
        if (bossBar != null) {
            bossBar.removeAll();
        }
        BukkitUtils.sendActionBar(player, "");
    }

    public void cancelTasks(Player player) {
        String playerName = player.getName();
        BukkitTask task;

        task = authTimeoutTasks.remove(playerName);
        if (task != null) task.cancel();

        task = authReminderTasks.remove(playerName);
        if (task != null) task.cancel();

        task = displayTimerTasks.remove(playerName);
        if (task != null) task.cancel();

        clearDisplays(player);
    }
}
