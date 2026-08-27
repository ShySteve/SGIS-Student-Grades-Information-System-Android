package com.sgis.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Formats a stored timestamp as "Viewed 10 mins ago", used for the Teacher-facing grade view indicator. */
public class RelativeTime {
    public static String format(Long millis) {
        if (millis == null) return "Not yet viewed";
        long diff = Math.max(0, System.currentTimeMillis() - millis);
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "Viewed just now";
        if (minutes < 60) return "Viewed " + minutes + (minutes == 1 ? " min ago" : " mins ago");
        if (hours < 24) return "Viewed " + hours + (hours == 1 ? " hour ago" : " hours ago");
        if (days < 7) return "Viewed " + days + (days == 1 ? " day ago" : " days ago");
        return "Viewed on " + new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(millis));
    }
}
