package io.github.thebusybiscuit.slimefun4.utils;

import io.github.thebusybiscuit.cscorelib2.chat.ChatColors;
import io.github.thebusybiscuit.cscorelib2.chat.ChatInput;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.function.Consumer;

public final class ChatUtils {

    private ChatUtils() {
    }

    public static void sendURL(CommandSender sender, String url) {
        sender.sendMessage("");
        SlimefunPlugin.getLocalization().sendMessage(sender, "messages.link-prompt", false);
        sender.sendMessage(ChatColors.color("&7&o" + url));
        sender.sendMessage("");
    }

    public static String removeColorCodes(String string) {
        return ChatColor.stripColor(ChatColors.color(string));
    }

    public static String crop(ChatColor color, String string) {
        if (ChatColor.stripColor(color + string).length() > 19) {
            return (color + ChatColor.stripColor(string)).substring(0, 18) + "...";
        } else {
            return color + ChatColor.stripColor(string);
        }
    }

    public static String christmas(String text) {
        return ChatColors.alternating(text, ChatColor.GREEN, ChatColor.RED);
    }

    public static void awaitInput(Player p, Consumer<String> callback) {
        ChatInput.waitForPlayer(SlimefunPlugin.instance(), p, callback);
    }

    public static String humanize(String string) {
        StringBuilder builder = new StringBuilder();

        String[] segments = PatternUtils.UNDERSCORE.split(string.toLowerCase(Locale.ROOT));

        builder.append(Character.toUpperCase(segments[0].charAt(0))).append(segments[0].substring(1));

        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            builder.append(' ').append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
        }

        return builder.toString();
    }

}
