package com.plex.giveaways.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlexText {
    private static final Pattern HEX_TAG_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern STRIP_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-ORX]");
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private PlexText() {}

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_TAG_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char ch : hex.toCharArray()) {
                replacement.append('§').append(ch);
            }
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(builder);
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    public static Component component(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return SERIALIZER.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static String strip(String message) {
        if (message == null) {
            return "";
        }
        return STRIP_PATTERN.matcher(colorize(message)).replaceAll("");
    }

    public static String safeReplace(String source, String target, String replacement) {
        if (source == null) return "";
        if (target == null || replacement == null) return source;
        return source.replace(target, replacement);
    }
}
