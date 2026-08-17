package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.hash.HashFactory;

import java.util.regex.Pattern;

public class ReloadCommand implements AdminSubcommand {
    private final TiAuth plugin;

    public ReloadCommand(TiAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "tiauth.admin.commands.reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MainConfig.IMP.reload();
        MessagesConfig.IMP.reload();
        plugin.getAuthManager().setPasswordPattern(Pattern.compile(MainConfig.IMP.auth.passwordPattern));
        plugin.getAuthManager().setHash(HashFactory.create(MainConfig.IMP.auth.hashAlgorithm));
        plugin.getAutoBackupManager().restart();
        CachedMessages.IMP = new CachedMessages(MessagesConfig.IMP);
        BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.config.reload);
    }
}
