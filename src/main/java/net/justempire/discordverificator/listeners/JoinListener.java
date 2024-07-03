package net.justempire.discordverificator.listeners;

import net.justempire.discordverificator.DiscordVerificatorPlugin;
import net.justempire.discordverificator.exceptions.NoVerificationsFoundException;
import net.justempire.discordverificator.models.User;
import net.justempire.discordverificator.UserService;
import net.justempire.discordverificator.exceptions.SharedDiscordServerWasNotFoundException;
import net.justempire.discordverificator.exceptions.UserNotFoundException;
import net.justempire.discordverificator.utils.MessageColorizer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class JoinListener implements Listener {
    private UserService userService;
    private DiscordVerificatorPlugin plugin;

    public JoinListener(DiscordVerificatorPlugin plugin, UserService userService) {
        this.plugin = plugin;
        this.userService = userService;
    }

    @EventHandler
    public void onPlayerJoined(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String ipAddress = event.getAddress().getHostAddress();

        User user;

        try { user = userService.getByMinecraftUsername(player.getName());}
        catch (UserNotFoundException e) {
            String message = MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("account-not-linked"));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(message);
            event.disallow(event.getResult(), message);
            return;
        }

        // Check if IP is in ignore list
        if (user.isIpIgnored(ipAddress)) {
            String message = MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("ip-is-ignored"));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(message);
            event.disallow(event.getResult(), message);
            return;
        }

        // Check if bot is working
        if (!plugin.getDiscordBot().isBotEnabled()) {
            String message = MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("bot-not-working"));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(message);
            event.disallow(event.getResult(), message);
            return;
        }

        // Ensure that no more than 30 seconds have passed
        if (!user.getCurrentAllowedIp().equals(ipAddress)) {
            Date now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
            Date latestVerificationSent;
            try {
                latestVerificationSent = user.getLatestVerificationTimeFromIp(ipAddress);
                long differenceInSeconds = getDifferenceInSeconds(latestVerificationSent, now);
                if (differenceInSeconds < 30) {
                    String message = MessageColorizer.colorize(String.format(DiscordVerificatorPlugin.getMessage("wait-until-verification"), 30 - differenceInSeconds));
                    event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                    event.setKickMessage(message);
                    event.disallow(event.getResult(), message);
                    return;
                }
            } catch (NoVerificationsFoundException e) {
                try {
                    userService.updateLatestVerificationTimeFromIp(user.getDiscordId(), ipAddress);
                } catch (UserNotFoundException ignored) {
                }
            }
        }

        // Send verification to DM
        if (!user.getCurrentAllowedIp().equals(ipAddress)) {
            String message = MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("confirmation-sent"));

            try { plugin.getDiscordBot().sendVerificationToUser(user.getDiscordId(), ipAddress); }
            catch (SharedDiscordServerWasNotFoundException e)
            { message = MessageColorizer.colorize(DiscordVerificatorPlugin.getMessage("profile-not-found")); }

            try { userService.updateLatestVerificationTimeFromIp(user.getDiscordId(), ipAddress); }
            catch (UserNotFoundException ignored) { }

            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(message);
            event.disallow(event.getResult(), message);
        }
    }

    private static long getDifferenceInSeconds(Date startDate, Date endDate) {
        LocalDateTime startDateTime = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime endDateTime = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        Duration duration = Duration.between(startDateTime, endDateTime);
        return duration.getSeconds();
    }
}
