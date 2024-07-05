package net.justempire.discordverificator.commands;

import net.justempire.discordverificator.DiscordVerificatorPlugin;
import net.justempire.discordverificator.services.UserManager;
import net.justempire.discordverificator.exceptions.NotFoundException;
import net.justempire.discordverificator.utils.MessageColorizer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class UnlinkCommand implements CommandExecutor {
    private final UserManager userManager;

    public UnlinkCommand(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] arguments) {
        if (!commandSender.hasPermission("discordVerificator.unlink")) {
            commandSender.sendMessage(MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("not-enough-permissions")));
            return true;
        }

        if (arguments.length != 1) {
            commandSender.sendMessage(MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("invalid-unlink-format")));
            return true;
        }

        try {
            userManager.unlinkUser(arguments[0]);
            commandSender.sendMessage(MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("successfully-unlinked")));
            return true;
        }
        catch (NotFoundException e) {
            commandSender.sendMessage(MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("player-was-not-linked")));
            return true;
        }
    }
}
