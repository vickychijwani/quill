package me.vickychijwani.spectre.util;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {

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
