package me.vickychijwani.spectre.util;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {

    // 2114380800 seconds in epoch time == 01/01/2037 @ 12:00am (UTC)
    public static final Date FAR_FUTURE = new Date(2114380800L * 1000);

    public static long getEpochSeconds() {
        return System.currentTimeMillis() / 1000l;
    }

    /**
     * Converts a {@link Date} object to an ISO-formatted {@link String} representation of it.
     *
     * @param date  the {@link Date} to format. Must not be {@code null}.
     * @return      a formatted {@link String} of the form "yyyy-MM-dd HH:mm"
     */
    public static String dateToIsoDateString(@NonNull Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

}
