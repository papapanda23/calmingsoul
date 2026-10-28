package com.gathin.tvhome;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Screensaver in Home's style: the live valley (SceneView) following the real sun and the weather outside, the
 * clock with the day and date at the bottom right, and at the bottom left the city, the weather now and the next
 * five days. The place is config.xml's latitude, longitude and weather_city; the weather comes from Open-Meteo
 * every 30 minutes while it runs. Set with `settings put secure screensaver_components
 * com.gathin.tvhome/.AmbientDream`, or pick "Panda Home" in the TV's screensaver settings. The text drifts a few
 * pixels each minute so nothing stays in one place for hours.
 */
public class AmbientDream extends DreamService {
    private static final long MINUTE_MS = 60_000L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private SceneView scene;
    private View info, weatherBlock;
    private TextView nowTemp, nowSky;
    private WeatherIconView nowIcon;
    private LinearLayout forecast;
    private double latitude, longitude;
    private boolean hasPlace;
    private HandlerThread weatherThread;
    private Handler weatherBg;
    private int drift;

    private final Runnable minute = new Runnable() {
        @Override
        public void run() {
            if (hasPlace) scene.setPhase(SceneView.phaseNow(latitude, longitude));
            drift = (drift + 1) % 16;
            float d = getResources().getDisplayMetrics().density;
            info.setTranslationX(((drift % 4) - 1.5f) * 4 * d);
            info.setTranslationY(-(drift / 4) * 4 * d);
            main.postDelayed(this, MINUTE_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setContentView(R.layout.dream_ambient);
        scene = findViewById(R.id.scene);
        info = findViewById(R.id.info);
        weatherBlock = findViewById(R.id.weatherBlock);
        nowIcon = findViewById(R.id.nowIcon);
        nowTemp = findViewById(R.id.nowTemp);
        nowSky = findViewById(R.id.nowSky);
        forecast = findViewById(R.id.forecast);
        ((TextView) findViewById(R.id.city)).setText(getString(R.string.weather_city));
        try {
            latitude = Double.parseDouble(getString(R.string.latitude));
            longitude = Double.parseDouble(getString(R.string.longitude));
            hasPlace = true;
        } catch (NumberFormatException e) {
            hasPlace = false;
        }
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        scene.setRunning(true);
        main.post(minute);
        if (hasPlace) startWeather();
    }

    @Override
    public void onDreamingStopped() {
        stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void stop() {
        main.removeCallbacks(minute);
        if (scene != null) scene.setRunning(false);
        if (weatherThread != null) {
            weatherBg.removeCallbacksAndMessages(null);
            weatherThread.quit();
            weatherThread = null;
        }
    }

    private void startWeather() {
        if (weatherThread != null) return;
        weatherThread = new HandlerThread("dream-weather");
        weatherThread.start();
        Handler bg = new Handler(weatherThread.getLooper());
        weatherBg = bg;
        bg.post(new Runnable() {
            @Override
            public void run() {
                WeatherLine.Now now;
                try {
                    now = WeatherLine.fetch(latitude, longitude);
                } catch (Exception e) {
                    now = null;
                }
                WeatherLine.Now got = now;
                if (got != null) main.post(() -> show(got));
                bg.postDelayed(this, got == null ? WeatherLine.RETRY_MS : WeatherLine.REFRESH_MS);
            }
        });
    }

    private void show(WeatherLine.Now now) {
        scene.setWeather(now.code);
        nowIcon.set(now.code, now.day);
        nowTemp.setText(getString(R.string.dream_temp, now.tempC));
        nowSky.setText(titleCase(getString(WeatherLine.sky(now.code, now.day))));
        forecast.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        for (WeatherLine.Day day : now.days.subList(0, Math.min(5, now.days.size()))) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (forecast.getChildCount() > 0) lp.setMarginStart(Math.round(22 * d));
            cell.addView(text(day.date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    11, 0xD9FFFFFF), wrap());
            WeatherIconView icon = new WeatherIconView(this, null);
            icon.set(day.code, true);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Math.round(24 * d), Math.round(24 * d));
            ilp.topMargin = Math.round(3 * d);
            ilp.bottomMargin = Math.round(3 * d);
            cell.addView(icon, ilp);
            cell.addView(text(getString(R.string.dream_high_low, day.maxC, day.minC), 11, 0xFFFFFFFF), wrap());
            forecast.addView(cell, lp);
        }
        weatherBlock.setVisibility(View.VISIBLE);
    }

    /** A vertical LinearLayout's default is match_parent wide, which squeezed the high/low to the icon's width. */
    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(nowSky.getTypeface());
        t.setSingleLine(true);   // "31° / 21°" must not wrap its low onto a line the cell has no room for
        return t;
    }

    /** "Partly cloudy" -> "Partly Cloudy". */
    static String titleCase(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean start = true;
        for (char ch : s.toCharArray()) {
            out.append(start ? Character.toUpperCase(ch) : ch);
            start = Character.isWhitespace(ch);
        }
        return out.toString();
    }
}
