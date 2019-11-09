package de.comroid.twitchcord.irc;

import de.comroid.twitchcord.TwitchCore;

import org.jibble.pircbot.PircBot;

public class IRCBot extends PircBot {
    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        TwitchCore.INSTANCE.onIRCMessage(channel, sender, login, hostname, message);
    }
}
