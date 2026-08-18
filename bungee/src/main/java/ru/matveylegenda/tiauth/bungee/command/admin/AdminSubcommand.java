package ru.matveylegenda.tiauth.bungee.command.admin;

import net.md_5.bungee.api.CommandSender;

import java.util.List;

public interface AdminSubcommand {
    String permission();

    void execute(CommandSender sender, String[] args);

    default List<String> suggest(CommandSender sender, String[] args) {
        return List.of();
    }
}
