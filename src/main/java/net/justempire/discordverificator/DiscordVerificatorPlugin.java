package net.justempire.discordverificator;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.justempire.discordverificator.commands.LinkCommand;
import net.justempire.discordverificator.commands.ReloadCommand;
import net.justempire.discordverificator.commands.UnlinkCommand;
import net.justempire.discordverificator.discord.DiscordBot;
import net.justempire.discordverificator.listeners.JoinListener;
import net.justempire.discordverificator.services.ConfirmationCodeService;
import net.justempire.discordverificator.services.UserManager;
import net.justempire.discordverificator.utils.MessageColorizer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DiscordVerificatorPlugin extends JavaPlugin {
    private Logger logger;
    private UserManager userManager;
    private ConfirmationCodeService confirmationCodeService;
    private DiscordBot discordBot;

    private JDA currentJDA;
    private static Map<String, String> messages = new HashMap<>();

    @Override
    public void onEnable() {
        // Creating a config if it doesn't exist
        saveDefaultConfig();

        // Setting up logger
        logger = this.getLogger();

        // Set up services
        userManager = new UserManager(String.format("%s/users.json", getDataFolder()));
        confirmationCodeService = new ConfirmationCodeService();

        // Setting up the bot
        setupBot();

        // Setting up the messages
        setupMessages();

        // Set up listeners
        getServer().getPluginManager().registerEvents(new JoinListener(this, userManager, confirmationCodeService), this);

        // Setting up commands
        LinkCommand linkCommand = new LinkCommand(userManager);
        getCommand("link").setExecutor(linkCommand);

        UnlinkCommand unlinkCommand = new UnlinkCommand(userManager);
        getCommand("unlink").setExecutor(unlinkCommand);

        ReloadCommand reloadCommand = new ReloadCommand(this);
        getCommand("dvreload").setExecutor(reloadCommand);

        logger.info("Enabled successfully!");
    }

    @Override
    public void onDisable() {
        userManager.onShutDown();
        if (currentJDA != null) currentJDA.shutdown();
        logger.info("Shutting down!");
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    private void setupBot() {
        String token = getConfig().getString("token");
        DiscordBot bot = new DiscordBot(logger, userManager, confirmationCodeService);

        try {
            this.currentJDA = JDABuilder.createLight(token)
                    .addEventListeners(bot)
                    .enableIntents(
                            List.of(GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGE_TYPING)
                    )
                    .setAutoReconnect(true)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setStatus(OnlineStatus.ONLINE)
                    .build();
        }
        catch (LoginException e)
        { logger.severe(MessageColorizer.colorize("Wrong discord bot token provided!")); }

        this.discordBot = bot;
    }

    public void reload() {
        try { currentJDA.shutdownNow(); }
        catch (Exception ignore) { }
        reloadConfig();
        messages = new HashMap<>();
        setupMessages();
        userManager.reload();
        setupBot();
    }

    private void setupMessages() {
        // Getting the messages from the config
        ConfigurationSection configSection = getConfig().getConfigurationSection("messages");
        if (configSection != null) {
            // Adding these messages to dictionary
            Map<String, Object> messages = configSection.getValues(true);
            for (Map.Entry<String, Object> pair : messages.entrySet()) {
                DiscordVerificatorPlugin.messages.put(pair.getKey(), pair.getValue().toString());
            }
        }

        saveDefaultConfig();
    }

    // Returns a message from the config by key
    public static String getMessage(String key) {
        if (messages == null) return String.format("Message %s wasn't found (messages list is null)", key);
        if (messages.get(key) == null) return String.format("Message %s wasn't found", key);

        return MessageColorizer.colorize(messages.get(key));
    }
}

