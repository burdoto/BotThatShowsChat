package de.kaleidox.twitchcord.command;

import java.awt.Color;

import de.kaleidox.javacord.util.commands.Command;
import de.kaleidox.javacord.util.commands.CommandGroup;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;
import de.kaleidox.twitchcord.TwitchCore;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;

@CommandGroup(name = "Twitch Commands", description = "Commands for setting up Twitch connections")
public enum TwitchCommands {
    INSTANCE;

    @Command(
            aliases = {"link", "unlink"},
            usage = "link <Twitch-Channel>",
            description = "Toggles a link of a Twitch-Channel to a text channel.",
            enablePrivateChat = false,
            requiredArguments = 1,
            requiredDiscordPermission = PermissionType.MANAGE_SERVER
    )
    public EmbedBuilder link(ServerTextChannel stc, String[] args) {
        if (args.length == 0) return new EmbedBuilder()
                .setColor(Color.RED)
                .setDescription("No channel name set!");
        String channelName = "#" + args[0];

        boolean active = TwitchCore.INSTANCE.tSubscription(channelName, stc);

        if (active) return DefaultEmbedFactory.create()
                .setDescription("Channel " + args[0] + " was linked to this channel!");
        else return DefaultEmbedFactory.create()
                .setDescription("Channel " + args[0] + " was unlinked from this channel!");
    }
}
