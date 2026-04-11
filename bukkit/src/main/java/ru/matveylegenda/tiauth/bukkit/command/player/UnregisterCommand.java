package ru.matveylegenda.tiauth.bukkit.command.player;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.matveylegenda.tiauth.bukkit.TiAuth;
import ru.matveylegenda.tiauth.bukkit.manager.AuthManager;
import ru.matveylegenda.tiauth.bukkit.storage.CachedMessages;
import ru.matveylegenda.tiauth.bukkit.util.BukkitUtils;

public class UnregisterCommand implements CommandExecutor {
    private final AuthManager authManager;

    public UnregisterCommand(TiAuth plugin) {
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            BukkitUtils.sendMessage(sender, CachedMessages.IMP.onlyPlayer);
            return true;
        }

        if (args.length != 1) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.unregister.usage);
            return true;
        }

        String password = args[0];
        authManager.unregisterPlayer(player, password);
        return true;
    }
}
