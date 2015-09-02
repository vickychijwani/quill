package me.vickychijwani.spectre.util;

import android.support.annotation.Nullable;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

public class DateTimeUtils {

    // 2114380800 seconds in epoch time == 01/01/2037 @ 12:00am (UTC)
    public static final Date FAR_FUTURE = new Date(2114380800L * 1000);

    private static final PrettyTime prettyTime = new PrettyTime();

    public static long getEpochSeconds() {
        return System.currentTimeMillis() / 1000l;
    }

    /**
     * Formats a {@link Date} relative to now. Examples: "moments ago", "3 days ago".
     *
     * @param date  the {@link Date} to format. Must not be {@code null}.
     * @return      a relative datetime string like "3 weeks ago"
     */
    public static String formatRelative(@Nullable Date date) {
        if (date == null) throw new IllegalArgumentException("date cannot be null!");
        return prettyTime.format(date);
    }

}
