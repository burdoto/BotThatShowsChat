package de.comroid.twitchcord;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import de.comroid.BotThatShowsChat;
import de.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import de.comroid.util.files.FileProvider;
import de.comroid.util.interfaces.Initializable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.util.logging.ExceptionLogger;

public enum TwitchCore implements Initializable, Closeable {
    INSTANCE;

    public static final File STORAGE = FileProvider.getFile("data/connections.json");

    private Map<String, Set<Long>> ircSubscriptionsMap = new ConcurrentHashMap<>();

    public void onIRCMessage(String channel, String sender, String login, String hostname, String message) {
        ircSubscriptionsMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(channel))
                .flatMap(entry -> entry.getValue().stream())
                .map(BotThatShowsChat.API::getServerTextChannelById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(stc -> stc.sendMessage(
                        DefaultEmbedFactory.create()
                                .setAuthor(sender)
                                .setDescription(message)
                                .setUrl("http://twitch.tv/" + channel.substring(1))
                                .setFooter("Twitch Chat: " + channel))
                        .exceptionally(ExceptionLogger.get())
                );
    }

    public boolean tSubscription(String twitchChannel, ServerTextChannel channel) {
        final boolean[] yield = {false};

        ircSubscriptionsMap.compute(twitchChannel, (k, v) -> {
            if (v == null) v = new ConcurrentSkipListSet<>();
            if (!v.remove(channel.getId()))
                yield[0] = v.add(channel.getId());
            return v;
        });

        if (yield[0]) BotThatShowsChat.IRC.joinChannel(twitchChannel);
        else BotThatShowsChat.IRC.partChannel(twitchChannel);

        return yield[0];
    }

    @Override
    public void init() throws IOException {
        JsonNode data = new ObjectMapper().readTree(STORAGE);

        if (data == null) return;

        Iterator<String> twitchNames = data.fieldNames();
        twitchNames.forEachRemaining(twitchName -> {
            ArrayNode connectedChannels = (ArrayNode) data.get(twitchName);

            connectedChannels.forEach(channelIdNode -> {
                long channelId = channelIdNode.asLong();

                BotThatShowsChat.API.getServerTextChannelById(channelId)
                        .ifPresent(serverTextChannel -> tSubscription(twitchName, serverTextChannel));
            });
        });
    }

    public void tick() throws IOException {
        ObjectNode data = JsonNodeFactory.instance.objectNode();

        ircSubscriptionsMap.forEach((twitchName, channelIdSet) -> {
            ArrayNode connectedChannels = data.has(twitchName)
                    ? (ArrayNode) data.get(twitchName)
                    : data.putArray(twitchName);

            channelIdSet.forEach(connectedChannels::add);
        });

        FileWriter writer = new FileWriter(STORAGE);
        writer.write(data.toString());
        writer.close();
    }

    public void cleanup() {
        ircSubscriptionsMap.keySet()
                .forEach(key -> ircSubscriptionsMap.compute(key, (k, v) -> {
                    if (v == null || v.size() == 0) return null;
                    return v;
                }));
    }

    @Override
    public void close() throws IOException {
        cleanup();

        tick();
    }
}
