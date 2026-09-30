package com.plex.giveaways.util;

import java.util.concurrent.TimeUnit;

public final class TimeFormatter {
    private TimeFormatter() {}

    public static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }
        long days = TimeUnit.SECONDS.toDays(totalSeconds);
        long hours = TimeUnit.SECONDS.toHours(totalSeconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public static String formatDigital(long totalSeconds) {
        if (totalSeconds <= 0) return "00:00";
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
