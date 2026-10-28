import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Draws the charts for docs/validation.md from tools/backtest/out/*.csv (made by analyze.py).
 *   java tools/backtest/Charts.java
 * JDK only (Java2D).
 */
public class Charts {
    static final Color INK = new Color(0x1f2933), MUTED = new Color(0x5f6b76), GRID = new Color(0xd9dee3);
    static final Color[] LEVEL = {new Color(0xe3eaf0), new Color(0xf2c14e), new Color(0xe8743b), new Color(0xb3261e)};
    static final String[] LNAME = {"Low", "Moderate", "High", "Severe"};
    static final String[] LCH = {"L", "M", "H", "S"};
    static Path root;

    public static void main(String[] a) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path here = Paths.get(System.getProperty("user.dir"));
        root = here;
        while (root != null && !Files.isDirectory(root.resolve("tools/backtest"))) root = root.getParent();
        Files.createDirectories(root.resolve("docs/validation"));
        heatmap();
        distribution();
        skill();
        eventRain();
    }

    static List<String[]> csv(String name) throws Exception {
        List<String[]> out = new ArrayList<>();
        for (String l : Files.readAllLines(root.resolve("tools/backtest/out/" + name))) out.add(l.split(",", -1));
        return out.subList(1, out.size());
    }

    static BufferedImage canvas(int w, int h, Graphics2D[] g) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        g[0] = g2;
        return img;
    }

    static void text(Graphics2D g, String s, float x, float y, float size, Color c, boolean bold, int align) {
        g.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, Math.round(size)));
        g.setColor(c);
        int w = g.getFontMetrics().stringWidth(s);
        g.drawString(s, align == 0 ? x : align == 1 ? x - w / 2f : x - w, y);
    }

    static void save(BufferedImage img, String name) throws Exception {
        ImageIO.write(img, "png", root.resolve("docs/validation/" + name).toFile());
        System.out.println("wrote docs/validation/" + name);
    }

    // 1. Level assigned by each forecast issued 6..0 days before each event.
    static void heatmap() throws Exception {
        String[] sources = {"ecmwf_ifs025", "gfs_seamless", "era5"};
        String[] title = {"ECMWF IFS forecast", "GFS forecast", "ERA5 (perfect forecast)"};
        Map<String, String> locName = new HashMap<>();
        locName.put("c47", "Nairobi HQ"); locName.put("c34", "Kajiado HQ"); locName.put("x_kitengela", "Kitengela site");
        locName.put("c32", "Nakuru HQ"); locName.put("x_maimahiu", "Mai Mahiu site"); locName.put("c42", "Kisumu HQ");
        locName.put("c7", "Garissa HQ"); locName.put("c4", "Tana River HQ");
        Map<String, String> evDate = new HashMap<>();
        evDate.put("E1", "24 Apr"); evDate.put("E2", "24 Apr"); evDate.put("E3", "29 Apr"); evDate.put("E4", "30 Apr");
        evDate.put("E5", "5 May"); evDate.put("E6", "6 May"); evDate.put("E7", "11 May");
        List<String[]> rows = csv("event_levels.csv");
        List<String> keys = new ArrayList<>();
        for (String[] r : rows) { String k = r[0] + "|" + r[1]; if (!keys.contains(k)) keys.add(k); }
        int cell = 34, left = 250, top = 110, gap = 28, panel = 7 * cell;
        int w = left + 3 * panel + 2 * gap + 30, h = top + keys.size() * cell + 120;
        Graphics2D[] gg = new Graphics2D[1];
        BufferedImage img = canvas(w, h, gg);
        Graphics2D g = gg[0];
        text(g, "Risk level the real RiskEngine assigned, by how many days before the event the forecast was issued", 20, 30, 17, INK, true, 0);
        text(g, "Default thresholds, March-May 2024. 0 = issued on the event day. Cell letters: L Low, M Moderate, H High, S Severe.", 20, 52, 13, MUTED, false, 0);
        for (int s = 0; s < 3; s++) {
            int x0 = left + s * (panel + gap);
            text(g, title[s], x0 + panel / 2f, top - 34, 14, INK, true, 1);
            for (int k = 0; k < 7; k++) text(g, String.valueOf(6 - k), x0 + k * cell + cell / 2f, top - 10, 12, MUTED, false, 1);
            for (int i = 0; i < keys.size(); i++) {
                String[] kk = keys.get(i).split("\\|");
                for (String[] r : rows) {
                    if (!(r[0].equals(kk[0]) && r[1].equals(kk[1]) && r[2].equals(sources[s]))) continue;
                    for (int k = 0; k < 7; k++) {
                        int lv = Integer.parseInt(r[3 + k]);
                        g.setColor(LEVEL[lv]);
                        g.fillRect(x0 + k * cell, top + i * cell, cell - 2, cell - 2);
                        text(g, LCH[lv], x0 + k * cell + (cell - 2) / 2f, top + i * cell + 22, 13, lv == 3 ? Color.WHITE : INK, true, 1);
                    }
                }
            }
        }
        for (int i = 0; i < keys.size(); i++) {
            String[] kk = keys.get(i).split("\\|");
            text(g, kk[0] + " " + evDate.get(kk[0]) + "  " + locName.get(kk[1]), left - 10, top + i * cell + 22, 13, INK, false, 2);
        }
        text(g, "Days before the event the forecast was issued", left + (3 * panel + 2 * gap) / 2f, top + keys.size() * cell + 28, 12, MUTED, false, 1);
        int lx = left;
        for (int l = 0; l < 4; l++) {
            g.setColor(LEVEL[l]); g.fillRect(lx, top + keys.size() * cell + 48, 18, 18);
            text(g, LNAME[l], lx + 24, top + keys.size() * cell + 62, 13, INK, false, 0);
            lx += 110;
        }
        text(g, "Data: Open-Meteo (CC BY 4.0). A High or Severe cell in the days before an event counts as a hit.", 20, h - 14, 11, MUTED, false, 0);
        save(img, "event-levels.png");
    }

    // 2. Share of county-days at each level, per input and threshold set.
    static void distribution() throws Exception {
        List<String[]> rows = csv("level_distribution.csv");
        int w = 980, h = 560, left = 80, top = 90, bottom = 150, ph = h - top - bottom;
        Graphics2D[] gg = new Graphics2D[1];
        BufferedImage img = canvas(w, h, gg);
        Graphics2D g = gg[0];
        text(g, "How often each level is shown (47 county HQs x 78 forecast days)", 20, 30, 17, INK, true, 0);
        text(g, "Share of county-days. Low (the remainder, 59-75 %) is not drawn. Scenarios change only the thresholds.", 20, 52, 13, MUTED, false, 0);
        double max = 45;
        for (int t = 0; t <= 45; t += 10) {
            float y = top + ph - (float) (t / max * ph);
            g.setColor(GRID); g.drawLine(left, (int) y, w - 20, (int) y);
            text(g, t + "%", left - 8, y + 4, 11, MUTED, false, 2);
        }
        String[] srcShort = {"ECMWF", "GFS", "ERA5"};
        String[] scName = {"default", "rain_lower", "rain_lower_river_x3"};
        int bw = 38, step = (w - left - 30) / 9;
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            double n = 0; for (int l = 2; l < 6; l++) n += Double.parseDouble(r[l]);
            float x = left + 10 + i * step, y = top + ph;
            for (int l = 1; l <= 3; l++) {
                double v = Double.parseDouble(r[2 + l]) / n * 100;
                float hh = (float) (v / max * ph);
                g.setColor(LEVEL[l]); g.fill(new Rectangle2D.Float(x, y - hh, bw, hh));
                if (hh > 14) text(g, String.format("%.1f", v), x + bw / 2f, y - hh / 2f + 4, 11, l == 3 ? Color.WHITE : INK, false, 1);
                y -= hh;
            }
            int src = i % 3;
            text(g, srcShort[src], x + bw / 2f, top + ph + 18, 12, INK, false, 1);
        }
        for (int s = 0; s < 3; s++) {
            float x = left + 10 + (s * 3 + 1) * step + 19;
            text(g, scName[s], x, top + ph + 42, 13, INK, true, 1);
        }
        text(g, "rain_lower = daily 15/35/60 mm and 3-day 30/60/100 mm.  river_x3 = river ratio High at 3x, Severe at 5x.", 20, top + ph + 74, 12, MUTED, false, 0);
        int lx = 20;
        for (int l = 1; l < 4; l++) {
            g.setColor(LEVEL[l]); g.fillRect(lx, top + ph + 92, 16, 16);
            text(g, LNAME[l], lx + 22, top + ph + 105, 13, INK, false, 0);
            lx += 110;
        }
        text(g, "Data: Open-Meteo (CC BY 4.0).", 20, h - 14, 11, MUTED, false, 0);
        save(img, "level-distribution.png");
    }

    // 3. How well the rainfall forecast itself found heavy days, by lead time.
    static void skill() throws Exception {
        List<String[]> rows = csv("skill_by_lead.csv");
        int w = 900, h = 580, left = 70, top = 90, bottom = 150, pw = w - left - 40, ph = h - top - bottom;
        Graphics2D[] gg = new Graphics2D[1];
        BufferedImage img = canvas(w, h, gg);
        Graphics2D g = gg[0];
        text(g, "Rainfall forecasts found only a minority of days with 20 mm or more", 20, 30, 17, INK, true, 0);
        text(g, "Against ERA5, 47 county HQ points, 152 days at or above 20 mm (14 Mar - 6 Jun 2024). Lead 0 = same-day run.", 20, 52, 13, MUTED, false, 0);
        for (int t = 0; t <= 100; t += 20) {
            float y = top + ph - t / 100f * ph;
            g.setColor(GRID); g.drawLine(left, (int) y, left + pw, (int) y);
            text(g, t + "%", left - 8, y + 4, 11, MUTED, false, 2);
        }
        for (int k = 0; k < 7; k++) text(g, String.valueOf(k), left + 30 + k * (pw - 60) / 6f, top + ph + 20, 12, MUTED, false, 1);
        text(g, "Lead time (days before the forecast day)", left + pw / 2f, top + ph + 42, 12, MUTED, false, 1);
        String[] models = {"ecmwf_ifs025", "gfs_seamless"};
        Color[] col = {new Color(0x1a6fb0), new Color(0xc2570c)};
        String[] lab = {"ECMWF IFS", "GFS"};
        for (int m = 0; m < 2; m++) for (int kind = 0; kind < 2; kind++) {
            Path2D.Float p = new Path2D.Float();
            boolean first = true;
            for (String[] r : rows) {
                if (!r[0].equals(models[m])) continue;
                int k = Integer.parseInt(r[1]);
                double v = Double.parseDouble(kind == 0 ? r[3] : r[4]) * 100;
                float x = left + 30 + k * (pw - 60) / 6f, y = top + ph - (float) (v / 100 * ph);
                if (first) { p.moveTo(x, y); first = false; } else p.lineTo(x, y);
            }
            g.setColor(col[m]);
            g.setStroke(kind == 0 ? new BasicStroke(2.5f) : new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{6, 5}, 0));
            g.draw(p);
        }
        g.setStroke(new BasicStroke(1));
        int ly = top + ph + 68;
        for (int m = 0; m < 2; m++) {
            g.setColor(col[m]); g.setStroke(new BasicStroke(2.5f)); g.drawLine(left + m * 330, ly, left + m * 330 + 30, ly);
            text(g, lab[m] + ": share found (solid)", left + m * 330 + 36, ly + 4, 12, INK, false, 0);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{6, 5}, 0));
            g.drawLine(left + m * 330, ly + 18, left + m * 330 + 30, ly + 18);
            text(g, lab[m] + ": share of alarms that were wrong (dashed)", left + m * 330 + 36, ly + 22, 12, INK, false, 0);
        }
        text(g, "Data: Open-Meteo previous-runs API and ERA5 (CC BY 4.0).", 20, h - 12, 11, MUTED, false, 0);
        save(img, "forecast-skill.png");
    }

    // 4. Why the misses happen: rain at the event points versus the thresholds.
    static void eventRain() throws Exception {
        List<String[]> rows = csv("event_rain.csv");
        int w = 1000, h = 560, left = 70, top = 90, bottom = 150, pw = w - left - 30, ph = h - top - bottom;
        Graphics2D[] gg = new Graphics2D[1];
        BufferedImage img = canvas(w, h, gg);
        Graphics2D g = gg[0];
        text(g, "Rain at the event points was well below the single-day thresholds", 20, 30, 17, INK, true, 0);
        text(g, "Largest daily total at each event point. Forecast bars are for the event day, issued 1 day earlier. Lines: 20 / 50 / 80 mm.", 20, 52, 13, MUTED, false, 0);
        double max = 90;
        double[] th = {20, 50, 80};
        for (int t = 0; t <= 90; t += 10) {
            float y = top + ph - (float) (t / max * ph);
            g.setColor(GRID); g.drawLine(left, (int) y, left + pw, (int) y);
            text(g, t + "", left - 8, y + 4, 11, MUTED, false, 2);
        }
        for (int i = 0; i < th.length; i++) {
            float y = top + ph - (float) (th[i] / max * ph);
            g.setColor(LEVEL[i + 1]); g.setStroke(new BasicStroke(2f)); g.drawLine(left, (int) y, left + pw, (int) y);
            text(g, LNAME[i + 1], left + pw - 4, y - 4, 11, INK, true, 2);
        }
        g.setStroke(new BasicStroke(1));
        Color[] bc = {new Color(0x2a2f36), new Color(0x1a6fb0), new Color(0xc2570c)};
        int groupW = pw / rows.size(), bw = groupW / 4;
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            double[] v = {Double.parseDouble(r[3]), Double.parseDouble(r[4]), Double.parseDouble(r[6])};
            for (int b = 0; b < 3; b++) {
                float x = left + i * groupW + 6 + b * (bw + 2), hh = (float) (v[b] / max * ph);
                g.setColor(bc[b]); g.fill(new Rectangle2D.Float(x, top + ph - hh, bw, hh));
            }
            String lbl = r[1].replace("(Kajiado)", "").replace("(Nakuru)", "").trim();
            text(g, lbl, left + i * groupW + groupW / 2f - 4, top + ph + 18, 11, INK, false, 1);
            text(g, r[0] + " " + r[2].substring(5), left + i * groupW + groupW / 2f - 4, top + ph + 33, 11, MUTED, false, 1);
        }
        String[] ln = {"ERA5 reanalysis (largest day E-1..E+1)", "ECMWF IFS (1 day ahead)", "GFS (1 day ahead)"};
        for (int b = 0; b < 3; b++) {
            g.setColor(bc[b]); g.fillRect(left + b * 300, top + ph + 56, 16, 16);
            text(g, ln[b], left + b * 300 + 22, top + ph + 69, 12, INK, false, 0);
        }
        text(g, "Grid-cell values average rain over roughly 25-30 km, so they smooth out the intense local downpours behind flash floods.", 20, top + ph + 96, 12, MUTED, false, 0);
        text(g, "Data: Open-Meteo (CC BY 4.0).", 20, h - 12, 11, MUTED, false, 0);
        save(img, "event-rainfall.png");
    }
}
