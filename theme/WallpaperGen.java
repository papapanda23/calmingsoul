import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Draws the launcher's day-themed wallpapers, three per day, into out/<day>/ as 1920x1080 JPEGs.
 * Run: java WallpaperGen.java out
 * monday ocean, tuesday forest, wednesday sunset, thursday aurora, friday Jumat night, saturday galaxy,
 * sunday pastel. Kept dark at the top left and along the bottom, where the clock and the dock sit.
 */
public class WallpaperGen {
    static final int W = 1920, H = 1080;

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "out");
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (int d = 0; d < days.length; d++) {
            File dir = new File(out, days[d]);
            dir.mkdirs();
            for (int v = 0; v < 3; v++) {
                BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                Random r = new Random(1000L * d + v * 77L + 13);
                switch (d) {
                    case 0: ocean(g, r, v); break;
                    case 1: forest(g, r, v); break;
                    case 2: sunset(g, r, v); break;
                    case 3: aurora(g, r, v); break;
                    case 4: jumat(g, r, v); break;
                    case 5: galaxy(g, r, v); break;
                    default: pastel(g, r, v); break;
                }
                vignette(g);
                g.dispose();
                save(img, new File(dir, days[d] + "-" + (v + 1) + ".jpg"));
            }
        }
    }

    // ---- themes -------------------------------------------------------------------------------------

    static void ocean(Graphics2D g, Random r, int v) {
        Color[][] skies = {{c(0x0B1E3A), c(0x1D4E7A)}, {c(0x081428), c(0x14607A)}, {c(0x101A3C), c(0x2C5C8C)}};
        sky(g, skies[v][0], skies[v][1]);
        glow(g, W * (0.62 + 0.1 * v), H * 0.30, 420, new Color(255, 230, 190, 70));
        Color deep = c(0x041226), mid = c(0x0B3A5C);
        for (int i = 0; i < 6; i++) {
            float t = i / 5f;
            hills(g, r, H * (0.48 + 0.09 * i), 10 + 8 * i, 3 + i, mix(mid, deep, t), 1f);
        }
        stars(g, r, 40, 0.35, 120);
    }

    static void forest(Graphics2D g, Random r, int v) {
        Color[][] skies = {{c(0x0E2A22), c(0x4E7F68)}, {c(0x1A2C1E), c(0x86A06A)}, {c(0x0C1F24), c(0x3F7C78)}};
        sky(g, skies[v][0], skies[v][1]);
        glow(g, W * (0.7 - 0.15 * v), H * 0.28, 360, new Color(255, 245, 210, 60));
        for (int i = 0; i < 6; i++) {
            float t = i / 5f;
            Color col = mix(c(0x5E8A72), c(0x061410), t);
            hills(g, r, H * (0.38 + 0.11 * i), 60 - 6 * i, 2 + i, col, 1f);
            if (i >= 2) trees(g, r, H * (0.40 + 0.11 * i), col, 18 + 6 * i);
            mist(g, H * (0.40 + 0.11 * i), new Color(220, 240, 230, 18));
        }
    }

    static void sunset(Graphics2D g, Random r, int v) {
        Color[][] skies = {{c(0x2A1446), c(0xF08A4B)}, {c(0x1E1030), c(0xE0565B)}, {c(0x241A48), c(0xF2B45A)}};
        sky(g, skies[v][0], skies[v][1]);
        double sx = W * (0.55 + 0.12 * v), sy = H * 0.58;
        glow(g, sx, sy, 520, new Color(255, 200, 120, 110));
        g.setColor(new Color(255, 220, 160, 230));
        g.fill(new Ellipse2D.Double(sx - 70, sy - 70, 140, 140));
        for (int i = 0; i < 4; i++) {
            float t = i / 3f;
            hills(g, r, H * (0.62 + 0.1 * i), 40 - 6 * i, 2 + i, mix(c(0x5A2A50), c(0x120818), t), 1f);
        }
    }

    static void aurora(Graphics2D g, Random r, int v) {
        sky(g, c(0x030814), c(0x0B1A2E));
        stars(g, r, 420, 0.75, 255);
        Color[][] bands = {{new Color(80, 255, 170), new Color(120, 120, 255)},
                {new Color(90, 230, 255), new Color(190, 90, 255)},
                {new Color(120, 255, 120), new Color(60, 200, 255)}};
        for (int b = 0; b < 3; b++) {
            Color col = b % 2 == 0 ? bands[v][0] : bands[v][1];
            double base = H * (0.30 + 0.08 * b + 0.03 * r.nextDouble());
            double phase = r.nextDouble() * Math.PI * 2;
            for (int x = 0; x < W; x += 3) {
                double y = base + 70 * Math.sin(x / 260.0 + phase) + 30 * Math.sin(x / 90.0 + phase * 2);
                double h = 160 + 90 * Math.sin(x / 180.0 + phase);
                GradientPaint p = new GradientPaint((float) x, (float) (y - h), new Color(col.getRed(), col.getGreen(), col.getBlue(), 0),
                        (float) x, (float) y, new Color(col.getRed(), col.getGreen(), col.getBlue(), 46));
                g.setPaint(p);
                g.fill(new Rectangle2D.Double(x, y - h, 3, h + 6));
            }
        }
        hills(g, r, H * 0.80, 30, 3, c(0x050A10), 1f);
        trees(g, r, H * 0.82, c(0x03070B), 26);
    }

    static void jumat(Graphics2D g, Random r, int v) {
        Color[][] skies = {{c(0x061A16), c(0x0F3B33)}, {c(0x0A1028), c(0x1B2F55)}, {c(0x120A22), c(0x2A1F4A)}};
        sky(g, skies[v][0], skies[v][1]);
        stars(g, r, 220, 0.7, 220);
        pattern(g, new Color(212, 175, 95, 22));
        // crescent moon
        double mx = W * (0.72 - 0.08 * v), my = H * 0.24;
        glow(g, mx, my, 260, new Color(255, 230, 170, 60));
        Area moon = new Area(new Ellipse2D.Double(mx - 70, my - 70, 140, 140));
        moon.subtract(new Area(new Ellipse2D.Double(mx - 40, my - 86, 140, 140)));
        g.setColor(new Color(250, 225, 160));
        g.fill(moon);
        // mosque silhouette
        Color sil = c(0x02070A);
        double cx = W * (0.5 + 0.12 * (v - 1)), ground = H * 0.86;
        g.setColor(sil);
        g.fill(new Rectangle2D.Double(0, ground, W, H - ground));
        g.fill(new Rectangle2D.Double(cx - 260, ground - 150, 520, 150));
        g.fill(new Ellipse2D.Double(cx - 150, ground - 300, 300, 300));
        g.fill(new Ellipse2D.Double(cx - 330, ground - 210, 120, 120));
        g.fill(new Ellipse2D.Double(cx + 210, ground - 210, 120, 120));
        for (int s : new int[]{-1, 1}) {
            double tx = cx + s * 380;
            g.fill(new Rectangle2D.Double(tx - 16, ground - 420, 32, 420));
            g.fill(new Rectangle2D.Double(tx - 26, ground - 330, 52, 14));
            Path2D tip = new Path2D.Double();
            tip.moveTo(tx - 20, ground - 418);
            tip.lineTo(tx, ground - 480);
            tip.lineTo(tx + 20, ground - 418);
            tip.closePath();
            g.fill(tip);
        }
        Path2D finial = new Path2D.Double();
        finial.moveTo(cx - 6, ground - 296);
        finial.lineTo(cx, ground - 350);
        finial.lineTo(cx + 6, ground - 296);
        g.fill(finial);
        // lit windows
        g.setColor(new Color(255, 205, 120, 150));
        for (int i = -3; i <= 3; i++) {
            Shape win = new RoundRectangle2D.Double(cx + i * 60 - 12, ground - 110, 24, 50, 24, 24);
            g.fill(win);
        }
    }

    static void galaxy(Graphics2D g, Random r, int v) {
        sky(g, c(0x05030C), c(0x0C0718));
        Color[][] neb = {{new Color(140, 60, 220), new Color(40, 140, 255)},
                {new Color(230, 70, 140), new Color(90, 60, 230)},
                {new Color(40, 200, 220), new Color(160, 70, 210)}};
        for (int i = 0; i < 26; i++) {
            Color col = i % 2 == 0 ? neb[v][0] : neb[v][1];
            double x = W * (0.25 + 0.6 * r.nextDouble()), y = H * (0.15 + 0.55 * r.nextDouble());
            glow(g, x, y, 160 + r.nextInt(320), new Color(col.getRed(), col.getGreen(), col.getBlue(), 26));
        }
        stars(g, r, 900, 1.0, 255);
        for (int i = 0; i < 5; i++) {
            double x = W * r.nextDouble(), y = H * 0.75 * r.nextDouble();
            glow(g, x, y, 30, new Color(255, 255, 255, 90));
        }
    }

    static void pastel(Graphics2D g, Random r, int v) {
        Color[][] skies = {{c(0x2B2140), c(0x7A5C8E)}, {c(0x1F2A3E), c(0x6E8AA8)}, {c(0x3A2232), c(0x9A6A7A)}};
        sky(g, skies[v][0], skies[v][1]);
        Color[] bokeh = {new Color(255, 190, 210), new Color(190, 220, 255), new Color(255, 230, 180), new Color(200, 255, 220)};
        for (int i = 0; i < 70; i++) {
            Color col = bokeh[r.nextInt(bokeh.length)];
            double rad = 20 + r.nextInt(110);
            double x = W * r.nextDouble(), y = H * (0.05 + 0.7 * r.nextDouble());
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 18 + r.nextInt(30)));
            g.fill(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));
        }
        for (int i = 0; i < 3; i++) {
            float t = i / 2f;
            hills(g, r, H * (0.72 + 0.1 * i), 30, 2 + i, mix(c(0x5A4466), c(0x1A1222), t), 1f);
        }
    }

    // ---- pieces -------------------------------------------------------------------------------------

    static void sky(Graphics2D g, Color top, Color bottom) {
        g.setPaint(new GradientPaint(0, 0, top, 0, H, bottom));
        g.fillRect(0, 0, W, H);
    }

    static void glow(Graphics2D g, double x, double y, double radius, Color col) {
        RadialGradientPaint p = new RadialGradientPaint(new Point2D.Double(x, y), (float) radius, new float[]{0f, 1f},
                new Color[]{col, new Color(col.getRed(), col.getGreen(), col.getBlue(), 0)});
        g.setPaint(p);
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }

    /** A filled ridge line from left to right at height base, made of a few sine waves. */
    static void hills(Graphics2D g, Random r, double base, double amp, int waves, Color col, float alpha) {
        double[] f = new double[waves], p = new double[waves], a = new double[waves];
        for (int i = 0; i < waves; i++) {
            f[i] = (0.6 + r.nextDouble() * 2.5) * (i + 1) / W * Math.PI * 2;
            p[i] = r.nextDouble() * Math.PI * 2;
            a[i] = amp / (i + 1);
        }
        Path2D path = new Path2D.Double();
        path.moveTo(0, H);
        for (int x = 0; x <= W; x += 8) {
            double y = base;
            for (int i = 0; i < waves; i++) y += a[i] * Math.sin(x * f[i] + p[i]);
            path.lineTo(x, y);
        }
        path.lineTo(W, H);
        path.closePath();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(col);
        g.fill(path);
        g.setComposite(AlphaComposite.SrcOver);
    }

    static void trees(Graphics2D g, Random r, double base, Color col, int size) {
        g.setColor(col);
        for (int x = -20; x < W + 20; x += size / 2 + r.nextInt(size)) {
            double h = size * (1.6 + r.nextDouble() * 1.6);
            Path2D t = new Path2D.Double();
            t.moveTo(x - size * 0.45, base + 6);
            t.lineTo(x, base - h);
            t.lineTo(x + size * 0.45, base + 6);
            t.closePath();
            g.fill(t);
        }
    }

    static void mist(Graphics2D g, double y, Color col) {
        g.setPaint(new GradientPaint(0, (float) (y - 60), new Color(col.getRed(), col.getGreen(), col.getBlue(), 0),
                0, (float) y, col));
        g.fill(new Rectangle2D.Double(0, y - 60, W, 80));
    }

    static void stars(Graphics2D g, Random r, int n, double heightShare, int maxAlpha) {
        for (int i = 0; i < n; i++) {
            double x = W * r.nextDouble(), y = H * heightShare * r.nextDouble();
            double s = r.nextDouble() < 0.92 ? 1.2 + r.nextDouble() : 2.4 + r.nextDouble() * 1.4;
            g.setColor(new Color(255, 255, 255, 40 + r.nextInt(Math.max(1, maxAlpha - 40))));
            g.fill(new Ellipse2D.Double(x, y, s, s));
        }
    }

    /** Eight-pointed stars in a grid: a faint Islamic geometric lattice. */
    static void pattern(Graphics2D g, Color col) {
        g.setColor(col);
        g.setStroke(new BasicStroke(1.6f));
        int step = 120;
        for (int y = 0; y < H + step; y += step) {
            for (int x = 0; x < W + step; x += step) {
                Rectangle2D sq = new Rectangle2D.Double(x - 30, y - 30, 60, 60);
                g.draw(sq);
                Shape rot = AffineTransform.getRotateInstance(Math.PI / 4, x, y).createTransformedShape(sq);
                g.draw(rot);
            }
        }
    }

    static void vignette(Graphics2D g) {
        RadialGradientPaint p = new RadialGradientPaint(new Point2D.Double(W / 2.0, H / 2.0), W * 0.75f,
                new float[]{0.55f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)});
        g.setPaint(p);
        g.fillRect(0, 0, W, H);
        // Darker under the clock (top left) and the dock (bottom) so the text stays readable.
        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 120), 0, H * 0.28f, new Color(0, 0, 0, 0)));
        g.fillRect(0, 0, W, (int) (H * 0.28));
        g.setPaint(new GradientPaint(0, H * 0.6f, new Color(0, 0, 0, 0), 0, H, new Color(0, 0, 0, 140)));
        g.fillRect(0, (int) (H * 0.6), W, H);
    }

    static Color c(int rgb) {
        return new Color(rgb);
    }

    static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static void save(BufferedImage img, File file) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.9f);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(img, null, null), param);
        }
        writer.dispose();
    }
}
