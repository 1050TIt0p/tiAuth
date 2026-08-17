package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import ru.matveylegenda.tiauth.bungee.TiAuth;
import ru.matveylegenda.tiauth.bungee.storage.CachedMessages;
import ru.matveylegenda.tiauth.bungee.util.BungeeUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TiAuthCommand extends Command implements TabExecutor {
    private final Map<String, AdminSubcommand> subcommands = new LinkedHashMap<>();

    public TiAuthCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);

        register(new ReloadCommand(plugin), "reload");
        register(new UnregisterCommand(plugin), "unregister", "unreg");
        register(new ChangePasswordCommand(plugin), "changepassword", "changepass");
        register(new ForceLoginCommand(plugin), "forcelogin");
        register(new ForceRegisterCommand(plugin), "forceregister");
        register(new ForcePremiumCommand(plugin), "forcepremium");
        register(new MigrateCommand(plugin), "migrate");
        register(new BackupCommand(plugin), "backup");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.admin.usage);
            return;
        }

        AdminSubcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            return;
        }

        if (!sender.hasPermission(subcommand.permission())) {
            BungeeUtils.sendMessage(sender, CachedMessages.IMP.noPermission);
            return;
        }

        subcommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return subcommands.entrySet().stream()
                    .filter(entry -> sender.hasPermission(entry.getValue().permission()))
                    .map(Map.Entry::getKey)
                    .filter(name -> name.startsWith(prefix))
                    .toList();
        }

        if (args.length == 0) {
            return List.of();
        }

        AdminSubcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null || !sender.hasPermission(subcommand.permission())) {
            return List.of();
        }

        return subcommand.suggest(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private void register(AdminSubcommand subcommand, String... names) {
        for (String name : names) {
            subcommands.put(name, subcommand);
        }
    }
}
