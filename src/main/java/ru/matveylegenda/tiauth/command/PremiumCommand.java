package ru.matveylegenda.tiauth.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.PremiumCache;
import ru.matveylegenda.tiauth.database.Database;

public class PremiumCommand extends Command {
    private final TiAuth plugin;
    private final Database database;
    private final PremiumCache premiumCache;

    public PremiumCommand(TiAuth plugin, String name) {
        super(name);
        this.plugin = plugin;
        this.database = plugin.database;
        this.premiumCache = plugin.premiumCache;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
           sender.sendMessage("Команду может использовать только игрок");

           return;
        }

        if (premiumCache.isPremium(player.getName())) {
            database.getAuthUserRepository().setPremium(player.getName(), false);
            premiumCache.removePremium(player.getName());

            player.sendMessage("Премиум режим выключен");
        } else {
            database.getAuthUserRepository().setPremium(player.getName(), true);
            premiumCache.addPremium(player.getName());

            player.sendMessage("Премиум режим включен");
        }
    }
}
