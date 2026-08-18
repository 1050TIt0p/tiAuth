package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.hash.HashFactory;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

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
    public void execute(CommandSource sender, String[] args) {
        MainConfig.IMP.reload();
        MessagesConfig.IMP.reload();
        plugin.getAuthManager().setPasswordPattern(Pattern.compile(MainConfig.IMP.auth.passwordPattern));
        plugin.getAuthManager().setHash(HashFactory.create(MainConfig.IMP.auth.hashAlgorithm));
        plugin.getAutoBackupManager().restart();
        CachedComponents.IMP = new CachedComponents(MessagesConfig.IMP);
        VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.config.reload);
    }
}
