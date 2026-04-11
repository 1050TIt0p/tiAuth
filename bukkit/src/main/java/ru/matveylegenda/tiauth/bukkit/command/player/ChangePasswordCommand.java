package ru.matveylegenda.tiauth.bukkit.command.player;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.matveylegenda.tiauth.bukkit.TiAuth;
import ru.matveylegenda.tiauth.bukkit.manager.AuthManager;
import ru.matveylegenda.tiauth.bukkit.storage.CachedMessages;
import ru.matveylegenda.tiauth.bukkit.util.BukkitUtils;

public class ChangePasswordCommand implements CommandExecutor {
    private final AuthManager authManager;

    public ChangePasswordCommand(TiAuth plugin) {
        this.authManager = plugin.getAuthManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            BukkitUtils.sendMessage(sender, CachedMessages.IMP.onlyPlayer);
            return true;
        }

        if (args.length != 2) {
            BukkitUtils.sendMessage(player, CachedMessages.IMP.player.changePassword.usage);
            return true;
        }

        String oldPassword = args[0];
        String newPassword = args[1];

        authManager.changePasswordPlayer(player, oldPassword, newPassword);
        return true;
    }
}
