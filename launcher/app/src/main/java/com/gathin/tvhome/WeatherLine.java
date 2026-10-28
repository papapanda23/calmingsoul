package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * One line under the clock with the weather now (config.xml weather_line), from Open-Meteo, which needs no key. The
 * only thing sent is the latitude and longitude from config.xml. Every call blocks, so MainActivity makes it off the
 * UI thread, and only while Home is on screen.
 */
final class WeatherLine {
    static final long REFRESH_MS = 30 * 60_000L;
    static final long RETRY_MS = 5 * 60_000L;
    private static final int TIMEOUT_MS = 8_000;

    private WeatherLine() { }

    /** The weather now: whole degrees Celsius, the WMO weather code, day or night, and the next five days. */
    static final class Now {
        final int tempC;
        final int code;
        final boolean day;
        /** Tomorrow and the four days after it. */
        final java.util.List<Day> days;

        Now(int tempC, int code, boolean day, java.util.List<Day> days) {
            this.tempC = tempC;
            this.code = code;
            this.day = day;
            this.days = days;
        }
    }

    /** One forecast day: its date, the WMO code and the high and low in whole degrees Celsius. */
    static final class Day {
        final java.time.LocalDate date;
        final int code;
        final int maxC;
        final int minC;

        Day(java.time.LocalDate date, int code, int maxC, int minC) {
            this.date = date;
            this.code = code;
            this.maxC = maxC;
            this.minC = minC;
        }
    }

    static Now fetch(double latitude, double longitude) throws IOException, JSONException {
        String url = String.format(Locale.ROOT, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                + "&current=temperature_2m,weather_code,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=6&timezone=auto",
                latitude, longitude);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        try (InputStream in = connection.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int n; (n = in.read(buffer)) != -1; ) out.write(buffer, 0, n);
            JSONObject reply = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            JSONObject current = reply.getJSONObject("current");
            java.util.List<Day> days = new java.util.ArrayList<>();
            JSONObject daily = reply.optJSONObject("daily");
            if (daily != null) {
                org.json.JSONArray dates = daily.getJSONArray("time"), codes = daily.getJSONArray("weather_code"),
                        max = daily.getJSONArray("temperature_2m_max"), min = daily.getJSONArray("temperature_2m_min");
                for (int i = 1; i < dates.length(); i++) {   // index 0 is today, which the current reading covers
                    days.add(new Day(java.time.LocalDate.parse(dates.getString(i)), codes.getInt(i),
                            (int) Math.round(max.getDouble(i)), (int) Math.round(min.getDouble(i))));
                }
            }
            return new Now((int) Math.round(current.getDouble("temperature_2m")), current.getInt("weather_code"),
                    current.getInt("is_day") == 1, days);
        } finally {
            connection.disconnect();
        }
    }

    /** The words for a WMO weather code. */
    static int sky(int code, boolean day) {
        switch (code) {
            case 0: return day ? R.string.weather_sunny : R.string.weather_clear;
            case 1: return day ? R.string.weather_mostly_sunny : R.string.weather_mostly_clear;
            case 2: return R.string.weather_partly_cloudy;
            case 3: return R.string.weather_cloudy;
            case 45: case 48: return R.string.weather_fog;
            case 51: case 53: case 55: case 56: case 57: return R.string.weather_drizzle;
            case 61: return R.string.weather_light_rain;
            case 63: case 66: return R.string.weather_rain;
            case 65: case 67: return R.string.weather_heavy_rain;
            case 80: case 81: case 82: return R.string.weather_showers;
            default: return code >= 95 ? R.string.weather_thunderstorm : R.string.weather_cloudy;
        }
    }
}
