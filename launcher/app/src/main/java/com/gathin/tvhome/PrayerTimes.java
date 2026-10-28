package com.gathin.tvhome;

import java.time.LocalDate;

/**
 * The five daily prayer times for one place, computed on the TV (no network), with the Kemenag RI parameters used
 * in Indonesia: Subuh at the sun 20° below the horizon, Isya at 18°, Ashar by shadow factor 1 (Shafi'i), and 2
 * minutes of ihtiyat added to each, rounded up to the minute as Kemenag's tables are. The sun's position follows
 * the US Naval Observatory's approximate formulas, within a minute of Kemenag's tables in these latitudes.
 */
final class PrayerTimes {
    private static final double FAJR_ANGLE = 20.0;
    private static final double ISHA_ANGLE = 18.0;
    /** Sunset: the sun's upper edge on the horizon, with refraction. */
    private static final double SUNSET_ANGLE = 0.833;
    private static final double IHTIYAT_MINUTES = 2.0;

    private PrayerTimes() { }

    /**
     * Minutes after local midnight for Subuh, Dzuhur, Ashar, Maghrib and Isya on {@code date}, at latitude and
     * longitude in decimal degrees and a zone {@code utcOffsetHours} from UTC (7 for WIB).
     */
    static int[] minutes(LocalDate date, double latitude, double longitude, double utcOffsetHours) {
        // Julian day at local noon.
        double jd = date.toEpochDay() + 2440587.5 + 0.5 - utcOffsetHours / 24.0;
        double d = jd - 2451545.0;
        double g = Math.toRadians(fix(357.529 + 0.98560028 * d, 360));
        double q = fix(280.459 + 0.98564736 * d, 360);
        double l = Math.toRadians(fix(q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g), 360));
        double e = Math.toRadians(23.439 - 0.00000036 * d);
        double ra = fix(Math.toDegrees(Math.atan2(Math.cos(e) * Math.sin(l), Math.cos(l))) / 15.0, 24);
        double decl = Math.asin(Math.sin(e) * Math.sin(l));
        double eqt = q / 15.0 - ra;
        double noon = fix(12 + utcOffsetHours - longitude / 15.0 - eqt, 24);

        double lat = Math.toRadians(latitude);
        double asrAngle = -Math.toDegrees(Math.atan(1.0 / (1.0 + Math.tan(Math.abs(lat - decl)))));
        double[] hours = {
                noon - hourAngle(FAJR_ANGLE, lat, decl),
                noon,
                noon + hourAngle(asrAngle, lat, decl),
                noon + hourAngle(SUNSET_ANGLE, lat, decl),
                noon + hourAngle(ISHA_ANGLE, lat, decl),
        };
        int[] out = new int[hours.length];
        for (int i = 0; i < hours.length; i++) out[i] = (int) Math.ceil(hours[i] * 60 + IHTIYAT_MINUTES - 1e-6);
        return out;
    }

    /** Hours from solar noon until the sun stands {@code angle} degrees below the horizon (negative: above). */
    private static double hourAngle(double angle, double lat, double decl) {
        double cos = (-Math.sin(Math.toRadians(angle)) - Math.sin(decl) * Math.sin(lat))
                / (Math.cos(decl) * Math.cos(lat));
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos)))) / 15.0;
    }

    private static double fix(double a, double b) {
        a = a - b * Math.floor(a / b);
        return a < 0 ? a + b : a;
    }
}
