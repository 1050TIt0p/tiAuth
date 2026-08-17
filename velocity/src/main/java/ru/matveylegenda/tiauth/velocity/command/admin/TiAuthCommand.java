package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.storage.CachedComponents;
import ru.matveylegenda.tiauth.velocity.util.VelocityUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TiAuthCommand implements SimpleCommand {
    private final Map<String, AdminSubcommand> subcommands = new LinkedHashMap<>();

    public TiAuthCommand(TiAuth plugin) {
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
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.usage);
            return;
        }

        AdminSubcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.admin.usage);
            return;
        }

        if (!sender.hasPermission(subcommand.permission())) {
            VelocityUtils.sendMessage(sender, CachedComponents.IMP.noPermission);
            return;
        }

        subcommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return subcommands.entrySet().stream()
                    .filter(entry -> sender.hasPermission(entry.getValue().permission()))
                    .map(Map.Entry::getKey)
                    .filter(name -> name.startsWith(prefix))
                    .toList();
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
