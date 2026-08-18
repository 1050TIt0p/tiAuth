package ru.matveylegenda.tiauth.velocity.command.admin;

import com.velocitypowered.api.command.CommandSource;

import java.util.List;

public interface AdminSubcommand {
    String permission();

    void execute(CommandSource sender, String[] args);

    default List<String> suggest(CommandSource sender, String[] args) {
        return List.of();
    }
}
