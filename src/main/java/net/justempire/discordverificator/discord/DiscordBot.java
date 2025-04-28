package net.justempire.discordverificator.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.justempire.discordverificator.DiscordVerificatorPlugin;
import net.justempire.discordverificator.exceptions.InvalidCodeException;
import net.justempire.discordverificator.models.UsernameAndIp;
import net.justempire.discordverificator.services.ConfirmationCodeService;
import net.justempire.discordverificator.services.UserManager;
import net.justempire.discordverificator.exceptions.UserNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DiscordBot extends ListenerAdapter {
    private final Logger logger;
    private final UserManager userManager;
    private final ConfirmationCodeService confirmationCodeService;

    private boolean botEnabled = false;

    public DiscordBot(Logger logger, UserManager repository, ConfirmationCodeService confirmationCodeService) {
        this.logger = logger;
        this.userManager = repository;
        this.confirmationCodeService = confirmationCodeService;
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        logger.info("Shutting down the bot!");
        botEnabled = false;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        CommandListUpdateAction commands = event.getJDA().updateCommands();

        CommandData confirmCommandData = new CommandData("confirm", getMessage("confirm-command"));
        confirmCommandData.addOption(OptionType.STRING, "code", getMessage("verification-code-you-got"));
        commands.addCommands(confirmCommandData).complete();

        CommandData minecraftCommandData = new CommandData("mc", getMessage("minecraft-command-argument"));
        minecraftCommandData.addOption(OptionType.STRING, "command", getMessage("minecraft-command"));
        commands.addCommands(minecraftCommandData).complete();

        botEnabled = true;
        logger.info("Bot started!");
    }

    @Override
    public void onReconnected(@NotNull ReconnectedEvent event) {
        logger.info("Bot reconnected!");
        botEnabled = true;
    }

    public boolean isBotEnabled() {
        return botEnabled;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        // If command is "confirm"
        if (event.getName().equals("confirm")) onConfirmSlashCommand(event.getUser().getId(), event);
        else if (event.getName().equals("mc")) onMinecraftSlashCommand(event.getUser().getId(), event);
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        if (event.getComponentId().startsWith("choose.player.")) {
            String username = Objects.requireNonNull(event.getComponent()).getLabel();
            String command = event.getComponentId().split("/", 2)[1];
            processMinecraftSlashCommand(Bukkit.getPlayerExact(username), command, event);
        }
    }

    private void processMinecraftSlashCommand(@Nullable Player player, String command, Interaction interaction) {
        if (player == null || !player.isOnline()) {
            interaction.replyEmbeds(generateEmbed(
                            getMessage("minecraft-command-failed"),
                            getMessage("minecraft-command-fail-to-execute"),
                            0xF63B2D)
                    ).setEphemeral(true)
                    .complete();
            return;
        }
        JavaPlugin plugin = JavaPlugin.getPlugin(DiscordVerificatorPlugin.class);
        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            player.performCommand(command);
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                interaction.replyEmbeds(generateEmbed(
                                                getMessage("minecraft-command-success"),
                                                getMessage("minecraft-command-executed"),
                                                0x9ACD32)
                                        ).setEphemeral(true)
                                        .complete();
                            });
                        }
                );
    }

    private void onMinecraftSlashCommand(String discordId, @NotNull SlashCommandEvent event) {
        OptionMapping command = event.getOption("command");
        if (command == null) {
            MessageEmbed embed = generateEmbed(getMessage("invalid-usage"), getMessage("provide-minecraft-command"), 0xF63B2D);
            event.replyEmbeds(embed).setEphemeral(true).complete();
            return;
        }

        net.justempire.discordverificator.models.User user;
        try {
            user = userManager.getByDiscordId(discordId);
        } catch (UserNotFoundException e) {
            MessageEmbed embed = generateEmbed(getMessage("user-not-found"), getMessage("user-not-found-description"), 0xF63B2D);
            event.replyEmbeds(embed).setEphemeral(true).complete();
            return;
        }

        List<Player> players = new ArrayList<>();

        for (String username : user.linkedMinecraftUsernames) {
            Player player = Bukkit.getPlayer(username);
            if (player != null && player.isOnline())
                players.add(player);
        }

        if (players.size() > 1) {
            event.replyEmbeds(generateEmbed(
                            getMessage("error-occurred"),
                            getMessage("multiple-players-online"),
                            0xF63B2D))
                    .setEphemeral(true)
                    .addActionRow(
                        players.stream().map((p) -> Button.primary(
                                "choose.player." + p.getName() + "/" + command.getAsString(),
                                p.getName()
                        )).collect(Collectors.toList())
                    ).complete();
            return;
        }

        if (players.isEmpty()) {
            event.replyEmbeds(generateEmbed(
                        getMessage("error-occurred"),
                        getMessage("you-are-not-online"),
                        0xF63B2D))
                    .setEphemeral(true)
                    .complete();
            return;
        }

        processMinecraftSlashCommand(players.get(0), command.getAsString(), event);
    }

    private void onConfirmSlashCommand(String discordId, @NotNull SlashCommandEvent event) {
        // Getting the code from command arguments (options)
        OptionMapping code = event.getOption("code");

        // If code wasn't provided
        if (code == null) {
            MessageEmbed embed = generateEmbed(getMessage("invalid-usage"), getMessage("provide-code-please"), 0xF63B2D);
            event.replyEmbeds(embed).setEphemeral(true).complete();
            return;
        }

        // Trying to get code data
        UsernameAndIp codeData;
        try { codeData = confirmationCodeService.getDataByCodeAndRemove(code.getAsString()); }
        catch (InvalidCodeException e) {
            // Telling the user that code is invalid
            MessageEmbed embed = generateEmbed(getMessage("invalid-code"), getMessage("invalid-code-description"), 0xF63B2D);
            event.replyEmbeds(embed).setEphemeral(true).complete();
            return;
        }

        try {
            // Return if user tries to confirm someone else's code
            if (!userManager.getByMinecraftUsername(codeData.getUsername()).getDiscordId().equals(discordId)) {
                MessageEmbed embed = generateEmbed(getMessage("error-occurred"), getMessage("its-not-your-account"), 0xF63B2D);
                event.replyEmbeds(embed).setEphemeral(true).complete();
                return;
            }

            // Confirming the code
            confirmIp(discordId, codeData.getIpAddress());
            MessageEmbed embed = generateEmbed(
                    getMessage("allowed"),
                    String.format(getMessage("allowed-to-join-from-ip"), codeData.getIpAddress()),
                    0x9ACD32);

            event.replyEmbeds(embed).setEphemeral(true).complete();
        }
        catch (UserNotFoundException e) {
            // Send user the message if he was not found
            MessageEmbed embed = generateEmbed(getMessage("user-not-found"), getMessage("user-not-found-description"), 0xF63B2D);
            event.replyEmbeds(embed).setEphemeral(true).complete();
        }
    }

    private MessageEmbed generateEmbed(String title, String description, int color) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(title);
        builder.setDescription(description);
        builder.setColor(color);

        return builder.build();
    }

    private void confirmIp(String discordId, String ip) throws UserNotFoundException {
        userManager.updateIp(discordId, ip);
    }
    
    private String getMessage(String key) {
        return DiscordVerificatorPlugin.getMessage(key);
    }
}
