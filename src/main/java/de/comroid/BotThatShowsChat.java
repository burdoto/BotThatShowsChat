package de.comroid;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.comroid.eval.EvalCommand;
import de.comroid.twitchcord.TwitchCore;
import de.comroid.twitchcord.command.AdminCommands;
import de.comroid.twitchcord.command.BasicCommands;
import de.comroid.twitchcord.command.TwitchCommands;
import de.comroid.twitchcord.irc.IRCBot;
import de.comroid.util.files.FileProvider;
import de.comroid.util.files.OSValidator;
import de.kaleidox.botstats.BotListSettings;
import de.kaleidox.botstats.javacord.JavacordStatsClient;
import de.kaleidox.botstats.model.StatsClient;
import de.comroid.javacord.util.commands.CommandHandler;
import de.comroid.javacord.util.server.properties.ServerPropertiesManager;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;

public class BotThatShowsChat {
    public final static Color THEME = new Color(0x6441a5);

    public static final DiscordApi API;
    public static final CommandHandler CMD;
    public static final ServerPropertiesManager PROP;
    public static final StatsClient STATS;
    public static final IRCBot IRC;

    static {
        try {
            File file = FileProvider.getFile("login/token.cred");
            System.out.println("Looking for token file at " + file.getAbsolutePath());
            API = new DiscordApiBuilder()
                    .setToken(new BufferedReader(new FileReader(file)).readLine())
                    .login()
                    .exceptionally(ExceptionLogger.get())
                    .join();

            API.updateStatus(UserStatus.DO_NOT_DISTURB);
            API.updateActivity("Booting up...");

            BotListSettings settings = BotListSettings.builder()
                    .postStatsTester(OSValidator::isUnix)
                    .build();
            STATS = new JavacordStatsClient(settings, API);

            DefaultEmbedFactory.setEmbedSupplier(() -> new EmbedBuilder().setColor(THEME));

            CMD = new CommandHandler(API);
            CMD.prefixes = new String[]{"btsc!"};
            CMD.useDefaultHelp(null);
            CMD.registerCommands(BasicCommands.INSTANCE);
            CMD.registerCommands(TwitchCommands.INSTANCE);
            CMD.registerCommands(AdminCommands.INSTANCE);
            
            CMD.registerCommands(EvalCommand.INSTANCE);

            PROP = new ServerPropertiesManager(FileProvider.getFile("data/props.json"));
            PROP.usePropertyCommand(null, CMD);
            PROP.register("bot.prefix", CMD.prefixes[0])
                    .withDisplayName("Custom Command Prefix")
                    .withDescription("A custom prefix to call bot commands with");
            PROP.register("bot.commandchannel", -1)
                    .withDisplayName("Command Channel ID")
                    .withDescription("The ID of the only channel where the commands should be executed.\n" +
                            "If the ID is invalid, every channel is accepted.");

            CMD.withCustomPrefixProvider(PROP.getProperty("bot.prefix"));
            CMD.withCommandChannelProvider(PROP.getProperty("bot.commandchannel"));
            CMD.useBotMentionAsPrefix = true;

            IRC = new IRCBot();
            IRC.changeNick("discord_btsc");
            IRC.connect("irc.chat.twitch.tv", 6667, new BufferedReader(new FileReader(FileProvider.getFile("login/twitch.cred"))).readLine());

            TwitchCore.INSTANCE.init();

            API.getThreadPool().getScheduler()
                    .scheduleAtFixedRate(BotThatShowsChat::tick, 5, 5, TimeUnit.MINUTES);
            Runtime.getRuntime().addShutdownHook(new Thread(BotThatShowsChat::shutdown));


            API.updateActivity(ActivityType.LISTENING, CMD.prefixes[0] + "help");
            API.updateStatus(UserStatus.ONLINE);
        } catch (Exception e) {
            throw new RuntimeException("Error in initializer", e);
        }
    }

    public static void main(String[] args) {
    }

    private static void tick() {
        try {
            TwitchCore.INSTANCE.tick();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void shutdown() {
        try {
            TwitchCore.INSTANCE.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
