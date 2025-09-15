package ru.matveylegenda.tiauth.command.admin;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ru.matveylegenda.tiauth.TiAuth;
import ru.matveylegenda.tiauth.cache.AuthCache;
import ru.matveylegenda.tiauth.cache.SessionCache;
import ru.matveylegenda.tiauth.config.MessagesConfig;
import ru.matveylegenda.tiauth.manager.AuthManager;
import ru.matveylegenda.tiauth.util.Utils;

public class TiAuthCommand extends Command {
    private final TiAuth plugin;
    private final MessagesConfig messagesConfig;
    private final Utils utils;
    private final AuthManager authManager;
    private final AuthCache authCache;
    private final SessionCache sessionCache;

    public TiAuthCommand(TiAuth plugin, String name, String... aliases) {
        super(name, null, aliases);
        this.plugin = plugin;
        this.messagesConfig = plugin.getMessagesConfig();
        this.utils = plugin.getUtils();
        this.authManager = plugin.getAuthManager();
        this.authCache = plugin.getAuthCache();
        this.sessionCache = plugin.getSessionCache();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            utils.sendMessage(
                    sender,
                    messagesConfig.admin.usage
            );
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("tiauth.admin.commands.reload")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                plugin.loadConfigs();
                utils.sendMessage(
                        sender,
                        messagesConfig.admin.config.reload
                );
            }

            case "unregister", "unreg" -> {
                if (!sender.hasPermission("tiauth.admin.commands.unregister")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                if (args.length < 2) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.unregister.usage
                    );
                    return;
                }

                String playerName = args[1];
                authManager.unregisterPlayer(playerName, success -> {
                    if (!success) {
                        utils.sendMessage(
                                sender,
                                messagesConfig.queryError
                        );
                        return;
                    }

                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);
                    if (player != null) {
                        sessionCache.removePlayer(playerName);

                        utils.kickPlayer(
                                player,
                                messagesConfig.player.unregister.success
                        );
                    }

                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.unregister.success
                                    .replace("{player}", playerName)
                    );
                });
            }

            case "changepassword", "changepass" -> {
                if (!sender.hasPermission("tiauth.admin.commands.changepassword")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                if (args.length < 3) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.changePassword.usage
                    );
                    return;
                }

                String playerName = args[1];
                String password = args[2];
                authManager.changePasswordPlayer(playerName, password, success -> {
                    if (!success) {
                        utils.sendMessage(
                                sender,
                                messagesConfig.queryError
                        );
                        return;
                    }

                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.changePassword.success
                                    .replace("{player}", playerName)
                    );
                });
            }

            case "forcelogin" -> {
                if (!sender.hasPermission("tiauth.admin.commands.forcelogin")) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.noPermission
                    );
                    return;
                }

                if (args.length < 2) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.forceLogin.usage
                    );
                    return;
                }

                ProxiedPlayer player = plugin.getProxy().getPlayer(args[1]);
                if (player == null) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.playerNotFound
                    );
                    return;
                }

                if (authCache.isAuthenticated(player.getName())) {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.forceLogin.isAuthenticated
                                    .replace("{player}", player.getName())
                    );
                    return;
                }

                authManager.loginPlayer(player, () -> {
                    utils.sendMessage(
                            sender,
                            messagesConfig.admin.forceLogin.success
                                    .replace("{player}", player.getName())
                    );
                });
            }
        }
    }
}
