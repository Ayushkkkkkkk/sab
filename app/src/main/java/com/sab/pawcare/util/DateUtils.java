package com.sab.pawcare.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {
    private DateUtils() {}

    public static String todayKey() {
        return dateKey(System.currentTimeMillis());
    }

    public static String dateKey(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
    }

    public static String weekKey(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int week = c.get(Calendar.WEEK_OF_YEAR);
        int year = c.get(Calendar.YEAR);
        return year + "-W" + String.format(Locale.US, "%02d", week);
    }

    public static String formatTime(int minutesFromMidnight) {
        int h = minutesFromMidnight / 60;
        int m = minutesFromMidnight % 60;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(c.getTime());
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("EEE, d MMM yyyy • h:mm a", Locale.getDefault()).format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    public static long startOfMonth(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long endOfMonth(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfMonth(millis));
        c.add(Calendar.MONTH, 1);
        c.add(Calendar.MILLISECOND, -1);
        return c.getTimeInMillis();
    }

    public static int weekdayBit(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int dow = c.get(Calendar.DAY_OF_WEEK); // 1 Sunday
        return 1 << (dow - 1);
    }

    public static String weekdayLabel(int mask) {
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((mask & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(names[i]);
            }
        }
        return sb.length() == 0 ? "No days" : sb.toString();
    }

    public static long combineDateAndMinutes(long dateMillis, int minutes) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateMillis);
        c.set(Calendar.HOUR_OF_DAY, minutes / 60);
        c.set(Calendar.MINUTE, minutes % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
