package me.vickychijwani.spectre.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {

    private static final int EARLIEST_VALID_YEAR = 1980;

    /**
     * Get the current {@link Date}.
     */
    @NonNull
    public static Date getCurrentDate() {
        return Calendar.getInstance().getTime();
    }

    public static long getEpochSeconds() {
        return System.currentTimeMillis() / 1000l;
    }

    /**
     * @param date the date to validate
     * @return true if the given {@link java.util.Date} is considered valid within the app.
     */
    public static boolean isValidDate(@Nullable Date date) {
        if (date == null) return false;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) >= EARLIEST_VALID_YEAR;
    }

    /**
     * Converts a {@link Date} object to an ISO-formatted {@link String} representation of it.
     *
     * @param date  the {@link Date} to format. Must not be {@code null}.
     * @return      a formatted {@link String} of the form "yyyy-MM-dd HH:mm"
     * @see         #isoDateStringToDate(String)
     */
    public static String dateToIsoDateString(@NonNull Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    /**
     * Converts an ISO-formatted {@link String} representation of a date to a {@link Date} object.
     *
     * @return  a {@link Date} object corresponding to given date string
     * @see     #dateToIsoDateString(Date)
     */
    public static Date isoDateStringToDate(String dateString) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dateString);
    }

}
