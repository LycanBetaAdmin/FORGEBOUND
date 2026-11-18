import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * Turns painted concept art into a pixel icon.
 * args: in out x0,y0,x1,y1 size fgLum colours [outline=1]
 *  - crops the box, treats pixels darker than fgLum (luminance 0..255) as background,
 *  - fits the art into size x size (keeps aspect, centred), averaging the foreground of each cell
 *    (a cell is opaque when at least 45% of it is foreground),
 *  - fills enclosed holes, quantises to the given number of colours (k-means), adds a 1px dark outline.
 */
public class Downscale {
    static double lum(int c) { return 0.299 * ((c >> 16) & 255) + 0.587 * ((c >> 8) & 255) + 0.114 * (c & 255); }

    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File(a[0]));
        String[] b = a[2].split(",");
        int x0 = Integer.parseInt(b[0]), y0 = Integer.parseInt(b[1]), x1 = Integer.parseInt(b[2]), y1 = Integer.parseInt(b[3]);
        int n = Integer.parseInt(a[3]); double fg = Double.parseDouble(a[4]); int k = Integer.parseInt(a[5]);
        boolean outline = a.length <= 6 || !a[6].equals("0");
        // tight box around the foreground
        int bx0 = x1, by0 = y1, bx1 = x0, by1 = y0;
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) if (lum(img.getRGB(x, y)) >= fg) { bx0 = Math.min(bx0, x); bx1 = Math.max(bx1, x); by0 = Math.min(by0, y); by1 = Math.max(by1, y); }
        int inner = outline ? n - 2 : n;
        double cell = Math.max((bx1 - bx0 + 1) / (double) inner, (by1 - by0 + 1) / (double) inner);
        int gw = (int) Math.ceil((bx1 - bx0 + 1) / cell), gh = (int) Math.ceil((by1 - by0 + 1) / cell);
        int ox = (n - gw) / 2, oy = (n - gh) / 2;
        double[][][] col = new double[n][n][3]; boolean[][] on = new boolean[n][n]; double[][][] all = new double[n][n][3];
        for (int j = 0; j < gh; j++) for (int i = 0; i < gw; i++) {
            int sx0 = (int) (bx0 + i * cell), sy0 = (int) (by0 + j * cell), sx1 = (int) (bx0 + (i + 1) * cell), sy1 = (int) (by0 + (j + 1) * cell);
            double r = 0, g = 0, bl = 0, ar = 0, ag = 0, ab = 0; int c = 0, tot = 0;
            for (int y = sy0; y < Math.min(sy1, by1 + 1); y++) for (int x = sx0; x < Math.min(sx1, bx1 + 1); x++) {
                int p = img.getRGB(x, y); tot++;
                ar += (p >> 16) & 255; ag += (p >> 8) & 255; ab += p & 255;
                if (lum(p) >= fg) { r += (p >> 16) & 255; g += (p >> 8) & 255; bl += p & 255; c++; }
            }
            if (tot == 0) continue;
            int X = ox + i, Y = oy + j;
            all[Y][X] = new double[]{ar / tot, ag / tot, ab / tot};
            if (c >= 0.45 * tot) { on[Y][X] = true; col[Y][X] = new double[]{r / c, g / c, bl / c}; }
        }
        // fill enclosed holes with the cell's plain average (keeps dark interior lines)
        boolean[][] out = new boolean[n][n]; Deque<int[]> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) { q.add(new int[]{i, 0}); q.add(new int[]{i, n - 1}); q.add(new int[]{0, i}); q.add(new int[]{n - 1, i}); }
        while (!q.isEmpty()) { int[] p = q.poll(); int x = p[0], y = p[1]; if (x < 0 || y < 0 || x >= n || y >= n || out[y][x] || on[y][x]) continue; out[y][x] = true; q.add(new int[]{x + 1, y}); q.add(new int[]{x - 1, y}); q.add(new int[]{x, y + 1}); q.add(new int[]{x, y - 1}); }
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) if (!on[y][x] && !out[y][x]) { on[y][x] = true; col[y][x] = all[y][x]; }
        // drop single isolated pixels
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) if (on[y][x]) {
            int nb = 0; for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) { int X = x + dx, Y = y + dy; if ((dx != 0 || dy != 0) && X >= 0 && Y >= 0 && X < n && Y < n && on[Y][X]) nb++; }
            if (nb == 0) on[y][x] = false;
        }
        // k-means
        List<double[]> pts = new ArrayList<>();
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) if (on[y][x]) pts.add(col[y][x]);
        pts.sort(Comparator.comparingDouble(p -> 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]));
        double[][] cen = new double[k][];
        for (int i = 0; i < k; i++) cen[i] = pts.get((int) ((i + 0.5) * pts.size() / k)).clone();
        int[] asg = new int[pts.size()];
        for (int it = 0; it < 30; it++) {
            for (int p = 0; p < pts.size(); p++) { double bd = 1e18; for (int c = 0; c < k; c++) { double d = 0; for (int t = 0; t < 3; t++) d += Math.pow(pts.get(p)[t] - cen[c][t], 2); if (d < bd) { bd = d; asg[p] = c; } } }
            double[][] s = new double[k][3]; int[] cn = new int[k];
            for (int p = 0; p < pts.size(); p++) { for (int t = 0; t < 3; t++) s[asg[p]][t] += pts.get(p)[t]; cn[asg[p]]++; }
            for (int c = 0; c < k; c++) if (cn[c] > 0) for (int t = 0; t < 3; t++) cen[c][t] = s[c][t] / cn[c];
        }
        double[] darkest = cen[0]; for (double[] c : cen) if (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2] < 0.299 * darkest[0] + 0.587 * darkest[1] + 0.114 * darkest[2]) darkest = c;
        BufferedImage o = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) if (on[y][x]) {
            double bd = 1e18; double[] best = null;
            for (double[] c : cen) { double d = 0; for (int t = 0; t < 3; t++) d += Math.pow(col[y][x][t] - c[t], 2); if (d < bd) { bd = d; best = c; } }
            o.setRGB(x, y, 0xFF000000 | ((int) best[0] << 16) | ((int) best[1] << 8) | (int) best[2]);
        }
        if (outline) {
            int oc = 0xFF000000 | ((int) (darkest[0] * 0.45) << 16) | ((int) (darkest[1] * 0.45) << 8) | (int) (darkest[2] * 0.45);
            BufferedImage copy = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB); copy.setData(o.getData());
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) if ((copy.getRGB(x, y) >>> 24) == 0) {
                boolean edge = false;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { int X = x + d[0], Y = y + d[1]; if (X >= 0 && Y >= 0 && X < n && Y < n && (copy.getRGB(X, Y) >>> 24) != 0) edge = true; }
                if (edge) o.setRGB(x, y, oc);
            }
        }
        ImageIO.write(o, "png", new File(a[1]));
        System.out.println("cell " + String.format(Locale.ROOT, "%.1f", cell) + " grid " + gw + "x" + gh);
    }
}
