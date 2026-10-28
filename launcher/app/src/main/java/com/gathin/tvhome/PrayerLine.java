package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

/**
 * The prayer-time lines Home and the screensaver share: today's five times with the current one bold and white,
 * and "Next: Subuh in 2 h 45 min" under it. Times come from PrayerTimes, computed on the TV.
 */
final class PrayerLine {
    private static final int[] NAMES = {R.string.prayer_fajr, R.string.prayer_dhuhr, R.string.prayer_asr,
            R.string.prayer_maghrib, R.string.prayer_isha};

    private PrayerLine() { }

    /** Fills both lines for now at latitude and longitude; either view may be null. Call once a minute. */
    static void update(Context ctx, TextView line, TextView next, double latitude, double longitude) {
        LocalDate today = LocalDate.now();
        double offset = ZoneId.systemDefault().getRules().getOffset(Instant.now()).getTotalSeconds() / 3600.0;
        int[] times = PrayerTimes.minutes(today, latitude, longitude, offset);
        LocalTime clock = LocalTime.now();
        int now = clock.getHour() * 60 + clock.getMinute();

        if (line != null) {
            // The prayer whose time is running: the last one already begun, Isya from midnight until Subuh.
            int active = times.length - 1;
            for (int i = 0; i < times.length; i++) {
                if (times[i] <= now) active = i;
            }
            SpannableStringBuilder text = new SpannableStringBuilder();
            for (int i = 0; i < times.length; i++) {
                if (i > 0) text.append("   ");
                int start = text.length();
                text.append(ctx.getString(NAMES[i])).append(' ')
                        .append(String.format(Locale.ROOT, "%02d:%02d", times[i] / 60, times[i] % 60));
                if (i == active) {
                    text.setSpan(new ForegroundColorSpan(Color.WHITE), start, text.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            line.setText(text);
            line.setVisibility(View.VISIBLE);
        }

        if (next != null) {
            // The next one and how long until it; after Isya that is tomorrow's Subuh.
            int nowSeconds = clock.toSecondOfDay();
            int upcoming = -1;
            for (int i = 0; i < times.length; i++) {
                if (times[i] * 60 > nowSeconds) {
                    upcoming = i;
                    break;
                }
            }
            int target = upcoming >= 0 ? times[upcoming] * 60
                    : (PrayerTimes.minutes(today.plusDays(1), latitude, longitude, offset)[0] + 24 * 60) * 60;
            int left = (target - nowSeconds + 59) / 60;
            next.setText(ctx.getString(R.string.prayer_next, ctx.getString(NAMES[Math.max(upcoming, 0)]),
                    countdown(ctx, left / 60, left % 60)));
            next.setVisibility(View.VISIBLE);
        }
    }

    /** "2 h 45 min", "1 h", "45 min". */
    private static String countdown(Context ctx, int hours, int minutes) {
        StringBuilder s = new StringBuilder();
        if (hours > 0) s.append(ctx.getString(R.string.prayer_hours, hours));
        if (minutes > 0 || hours == 0) {
            if (s.length() > 0) s.append(' ');
            s.append(ctx.getString(R.string.prayer_minutes, minutes));
        }
        return s.toString();
    }
}
