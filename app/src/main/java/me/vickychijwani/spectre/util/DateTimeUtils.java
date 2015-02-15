package me.vickychijwani.spectre.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {

    public enum DateFallback {
        /** Fallback parameter denoting earliest possible date */
        EARLIEST,
    }

    /**
     * Earliest possible date used in the app.
     */
    @NonNull
    public static String getEarliestDateString() {
        return "1970-01-01 00:00";
    }

    /**
     * Get the current {@link Date}.
     */
    @NonNull
    public static Date getCurrentDate() {
        return Calendar.getInstance().getTime();
    }

    /**
     * Converts a {@link java.util.Date} object to an ISO-formatted {@link String} representation of it.
     *
     * @param date      the {@link java.util.Date} to format. Can be {@code null}.
     * @param fallback  if {@code date} is {@code null}, this parameter decides the return value.
     * @return          a formatted {@link String} of the form "yyyy-MM-dd HH:mm"
     * @throws          IllegalArgumentException - if {@code date} is {@code null} and
     *                  {@code fallback} is invalid
     * @see             #dateToIsoDateString(java.util.Date)
     * @see             #isoDateStringToDate(String)
     */
    @NonNull
    public static String dateToIsoDateString(@Nullable Date date, @NonNull DateFallback fallback)
            throws IllegalArgumentException {
        if (date != null) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
        } else if (fallback == DateFallback.EARLIEST) {
            return getEarliestDateString();
        }
        throw new IllegalArgumentException("date is null and fallback parameter is invalid!");
    }

    /**
     * Converts a {@link Date} object to an ISO-formatted {@link String} representation of it.
     *
     * @param date  the {@link Date} to format. Must not be {@code null}.
     * @return      a formatted {@link String} of the form "yyyy-MM-dd HH:mm"
     * @see         #dateToIsoDateString(Date, DateFallback)
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
     * @see     #dateToIsoDateString(Date, DateFallback)
     */
    public static Date isoDateStringToDate(String dateString) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dateString);
    }

}
