package de.kaleidox.twitchcord.irc;

import de.kaleidox.twitchcord.TwitchCore;

import org.jibble.pircbot.PircBot;

public class IRCBot extends PircBot {
    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        TwitchCore.INSTANCE.onIRCMessage(channel, sender, login, hostname, message);
    }
}
