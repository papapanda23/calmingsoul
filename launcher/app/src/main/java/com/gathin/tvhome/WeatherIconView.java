package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * A small weather symbol for a WMO code, drawn in code (no image files): sun or moon, cloud, rain, snow, fog or a
 * lightning bolt, in white with a warm sun. Used by the screensaver's weather block.
 */
public class WeatherIconView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();
    private int code = -1;
    private boolean day = true;

    public WeatherIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    void set(int code, boolean day) {
        if (this.code == code && this.day == day) return;
        this.code = code;
        this.day = day;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (code < 0) return;
        float s = Math.min(getWidth(), getHeight());
        c.save();
        c.translate((getWidth() - s) / 2f, (getHeight() - s) / 2f);
        c.scale(s / 24f, s / 24f);   // drawn on a 24 x 24 grid
        stroke.setStrokeWidth(1.6f);

        boolean clear = code == 0 || code == 1;
        boolean partly = code == 1 || code == 2;
        if (clear || partly) {
            if (partly) drawSunOrMoon(c, 8.5f, 8.5f, 4.2f);
            else drawSunOrMoon(c, 12, 12, 5.5f);
        }
        if (!clear || partly) {
            boolean wet = code >= 51;
            drawCloud(c, partly ? 13.5f : 12, partly ? 14.5f : 11.5f, wet || code == 3 ? 0xFFE6EAF0 : 0xFFFFFFFF);
        }
        stroke.setColor(0xFFBFE3FF);
        if (code == 45 || code == 48) {
            stroke.setColor(0xCCFFFFFF);
            c.drawLine(4, 18, 20, 18, stroke);
            c.drawLine(6, 21, 18, 21, stroke);
        } else if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            fill.setColor(Color.WHITE);
            for (float x : new float[]{8, 12, 16}) c.drawCircle(x, 20, 1.2f, fill);
        } else if (code >= 95) {
            fill.setColor(0xFFFFD54F);
            path.reset();
            path.moveTo(12.5f, 15.5f);
            path.lineTo(9.5f, 20.5f);
            path.lineTo(12f, 20.5f);
            path.lineTo(10.5f, 24f);
            path.lineTo(15f, 18.5f);
            path.lineTo(12.5f, 18.5f);
            path.lineTo(14f, 15.5f);
            path.close();
            c.drawPath(path, fill);
        } else if (code >= 51) {
            int drops = code == 51 || code == 53 || code == 61 || code == 80 ? 2 : 3;
            float[] xs = drops == 2 ? new float[]{10, 14.5f} : new float[]{8, 12, 16};
            for (float x : xs) c.drawLine(x, 18, x - 1.2f, 21.5f, stroke);
        }
        c.restore();
    }

    private void drawSunOrMoon(Canvas c, float cx, float cy, float r) {
        if (day) {
            fill.setColor(0xFFFFC94D);
            c.drawCircle(cx, cy, r, fill);
            stroke.setColor(0xFFFFC94D);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 4 * i;
                float x1 = cx + (float) Math.cos(a) * (r + 1.8f), y1 = cy + (float) Math.sin(a) * (r + 1.8f);
                float x2 = cx + (float) Math.cos(a) * (r + 3.4f), y2 = cy + (float) Math.sin(a) * (r + 3.4f);
                c.drawLine(x1, y1, x2, y2, stroke);
            }
        } else {
            // A crescent: the moon disc less an offset disc.
            path.reset();
            path.addCircle(cx, cy, r, Path.Direction.CW);
            Path cut = new Path();
            cut.addCircle(cx + r * 0.55f, cy - r * 0.35f, r * 0.85f, Path.Direction.CW);
            path.op(cut, Path.Op.DIFFERENCE);
            fill.setColor(0xFFF1EAD8);
            c.drawPath(path, fill);
        }
    }

    private void drawCloud(Canvas c, float cx, float cy, int color) {
        fill.setColor(color);
        c.drawCircle(cx - 3.6f, cy + 0.8f, 3.2f, fill);
        c.drawCircle(cx + 0.4f, cy - 1.2f, 4.4f, fill);
        c.drawCircle(cx + 4.2f, cy + 1.0f, 3.0f, fill);
        oval.set(cx - 6.8f, cy + 0.6f, cx + 7.2f, cy + 4.0f);
        c.drawRoundRect(oval, 1.7f, 1.7f, fill);
    }
}
