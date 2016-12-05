package me.vickychijwani.spectre.util;

import android.content.Context;
import android.support.annotation.NonNull;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    // 2114380800 seconds in epoch time == 01/01/2037 @ 12:00am (UTC)
    public static final Date FAR_FUTURE = new Date(2114380800L * 1000);

    private static PrettyTime prettyTime = null;

    private static java.text.DateFormat timeFormat = null;
    private static java.text.DateFormat dateFormat = null;

    public static long getEpochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * Formats a past or future {@link Date} relative to now.
     * Examples: "moments from now", "3 days ago".
     *
     * @param date  the {@link Date} to format. Must not be {@code null}.
     * @return      a relative datetime string like "3 weeks ago"
     */
    public static String formatRelative(@NonNull Date date) {
        if (prettyTime == null) {
            prettyTime = new PrettyTime(Locale.getDefault());
        }
        return prettyTime.format(date);
    }

    public static String formatAbsolute(@NonNull Date date, @NonNull Context context) {
        // get locale-specific formatters
        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
        // NOTE: be careful not to add any English words below since this should be a
        // locale-independent UI string!
        return timeFormat.format(date) + ", " + dateFormat.format(date);
    }

}
