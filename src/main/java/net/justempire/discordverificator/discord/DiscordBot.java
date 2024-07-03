package net.justempire.discordverificator.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.components.Button;
import net.justempire.discordverificator.DiscordVerificatorPlugin;
import net.justempire.discordverificator.UserService;
import net.justempire.discordverificator.exceptions.SharedDiscordServerWasNotFoundException;
import net.justempire.discordverificator.exceptions.UserNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class DiscordBot extends ListenerAdapter {
    private final List<Member> members = new ArrayList<>();
    private final Logger logger;
    private final UserService userService;
    private boolean botEnabled = false;

    public DiscordBot(Logger logger, UserService repository) {
        this.logger = logger;
        this.userService = repository;
    }

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        botEnabled = false;
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        logger.info("Shutting down the bot!");
        botEnabled = false;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        List<Guild> guildList = event.getJDA().getGuilds();

        for (Guild guild : guildList) {
            members.addAll(guild.getMembers());
        }

        botEnabled = true;
        logger.info("Bot started!");
    }

    public boolean isBotEnabled() {
        return botEnabled;
    }

    public void sendVerificationToUser(String discordId, String ip) throws SharedDiscordServerWasNotFoundException {
        Member targetMember = null;

        // Find the user
        for (Member member : members) {
            if (member.getUser().getId().equalsIgnoreCase(discordId)) {
                targetMember = member;
                break;
            }
        }

        if (targetMember == null) {
            throw new SharedDiscordServerWasNotFoundException();
        }

        EmbedBuilder msg = new EmbedBuilder();
        msg.setTitle(DiscordVerificatorPlugin.getMessage("verification"));
        msg.setDescription(String.format(DiscordVerificatorPlugin.getMessage("trying-to-join-from-ip"), ip));
        msg.setColor(0xCC397B);

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.success(String.format("confirm-%s", ip), DiscordVerificatorPlugin.getMessage("its-me")));
        buttons.add(Button.danger(String.format("ignore/30-%s", ip), DiscordVerificatorPlugin.getMessage("ignore-for-30-minutes")));
        buttons.add(Button.danger(String.format("ignore/120-%s", ip), DiscordVerificatorPlugin.getMessage("ignore-for-2-hours")));
        buttons.add(Button.danger(String.format("ignore/1440-%s", ip), DiscordVerificatorPlugin.getMessage("ignore-for-1-day")));

        targetMember.getUser().openPrivateChannel().flatMap(privateChannel ->
            privateChannel.sendMessageEmbeds(msg.build()).setActionRow(buttons)
        ).queue();
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (event.getButton() == null) throw new NullPointerException();

        String userId = event.getUser().getId();
        String buttonId = event.getButton().getId();

        String[] parts = buttonId.split("-");
        String actionName = parts[0];
        String ipAddress = parts[1];

        try {
            // Deleting buttons
            MessageEmbed original = event.getMessage().getEmbeds().get(0);
            event.getMessage().editMessageEmbeds(original).setActionRows().complete();

            if (actionName.equalsIgnoreCase("confirm")) {
                // Delete the message if IP address is ignored
                if (userService.isIpIgnored(userId, ipAddress)) {
                    event.getMessage().delete().queue();
                    return;
                }

                confirmIp(userId, ipAddress);

                // Changing the content of message
                EmbedBuilder msg = new EmbedBuilder();
                msg.setTitle(DiscordVerificatorPlugin.getMessage("allowed"));
                msg.setDescription(String.format(DiscordVerificatorPlugin.getMessage("allowed-to-join-from-ip"), ipAddress));
                msg.setColor(0x9ACD32);

                /*// Clearing DM
                deleteAllMessagesExceptOne(event.getChannel(), event.getMessage());*/

                try { event.getMessage().editMessageEmbeds(original).setEmbeds(msg.build()).queue(); }
                catch (IllegalStateException ignored) { }
            }
            else if (actionName.contains("ignore")) {
                // Delete the message if IP address is already ignored
                if (userService.isIpIgnored(userId, ipAddress)) {
                    event.getMessage().delete().queue();
                    return;
                }

                // Determining term to ignore IP
                int ignoreTerm = Integer.parseInt(actionName.split("/")[1]);
                Date ignoreUntil = Date.from(LocalDateTime.now().plus(ignoreTerm, ChronoUnit.MINUTES).atZone(ZoneId.systemDefault()).toInstant());

                ignoreIp(userId, ipAddress, ignoreUntil);

                // Updating contents of original message
                EmbedBuilder msg = new EmbedBuilder();
                msg.setTitle(DiscordVerificatorPlugin.getMessage("ip-ignored"));
                msg.setDescription(String.format(DiscordVerificatorPlugin.getMessage("ip-is-now-ignored"), ipAddress, ignoreTerm + "m"));
                msg.setColor(0x8DA399);

                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.danger(String.format("unIgnore-%s", ipAddress), DiscordVerificatorPlugin.getMessage("cancel")));

                /*// Clearing DM
                deleteAllMessagesExceptOne(event.getChannel(), event.getMessage());*/

                try { event.getMessage().editMessageEmbeds(original).setEmbeds(msg.build()).setActionRow(buttons).queue(); }
                catch (IllegalStateException ignored) { }
            }
            else if (actionName.equalsIgnoreCase("unIgnore")) {
                userService.unIgnoreIp(userId, ipAddress);
                event.getMessage().delete().queue();
            }
        }
        catch (UserNotFoundException e) {
            logger.warning(String.format("Discord user with ID `%s` wasn't found!", userId));
            event.reply(String.format(DiscordVerificatorPlugin.getMessage("user-with-id-not-found"), userId)).queue();
        }
    }

    private void deleteAllMessagesExceptOne(MessageChannel channel, Message messageToNotBeDeleted) {
        MessageHistory history = MessageHistory.getHistoryFromBeginning(channel).complete();
        List<Message> messages = history.getRetrievedHistory();
        for (Message message : messages){
            if (message.getId().equals(messageToNotBeDeleted.getId())) continue;
            if (message.getAuthor().isBot()) message.delete().queue();
        }
    }

    private void confirmIp(String userId, String ip) throws UserNotFoundException {
        userService.updateIp(userId, ip);
    }

    private void ignoreIp(String userId, String ip, Date ignoreUntil) throws UserNotFoundException {
        userService.ignoreIp(userId, ip, ignoreUntil);
    }
}
