import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * Recovers the pixel grid of an upscaled pixel-art picture.
 * args: in out [x0 y0 x1 y1 crop | -] [bgTolerance] [mergeTolerance] [forceCell]
 * Prints the detected cell size/phase, writes the grid image (transparent background) and out-preview.png (x10).
 */
public class Pixelize {
    static int[][] px;
    static int W, H;

    static double edgeStrength(boolean xAxis, int pos) {
        double s = 0;
        if (xAxis) { for (int y = 0; y < H; y++) s += dist(px[y][pos], px[y][pos - 1]); }
        else { for (int x = 0; x < W; x++) s += dist(px[pos][x], px[pos - 1][x]); }
        return s;
    }

    static double dist(int a, int b) {
        int dr = ((a >> 16) & 255) - ((b >> 16) & 255), dg = ((a >> 8) & 255) - ((b >> 8) & 255), db = (a & 255) - (b & 255);
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    static double[] findGrid(boolean xAxis, double force) {
        int n = xAxis ? W : H;
        double[] e = new double[n];
        for (int i = 1; i < n; i++) e[i] = edgeStrength(xAxis, i);
        double mean = Arrays.stream(e).sum() / n;
        double lo = force > 0 ? force - 0.3 : 3, hi = force > 0 ? force + 0.3 : 20;
        List<double[]> res = new ArrayList<>();
        double gmax = -1;
        for (double p = lo; p <= hi; p += 0.005) {
            double best = -1, bph = 0;
            for (double ph = 0; ph < p; ph += 0.1) {
                double s = 0; int c = 0;
                for (double t = ph; t < n - 1; t += p) { int i = (int) Math.round(t); if (i >= 1 && i < n) { s += e[i] - mean; c++; } }
                double score = s / Math.max(1, c);
                if (score > best) { best = score; bph = ph; }
            }
            res.add(new double[]{p, bph, best});
            gmax = Math.max(gmax, best);
        }
        for (double[] r : res) if (r[2] >= 0.8 * gmax) return r;
        return res.get(0);
    }

    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File(a[0]));
        int cx0 = 0, cy0 = 0, cx1 = img.getWidth(), cy1 = img.getHeight();
        if (a.length > 2 && !a[2].equals("-")) { String[] c = a[2].split(","); cx0 = Integer.parseInt(c[0]); cy0 = Integer.parseInt(c[1]); cx1 = Integer.parseInt(c[2]); cy1 = Integer.parseInt(c[3]); }
        double bgTol = a.length > 3 ? Double.parseDouble(a[3]) : 40, mergeTol = a.length > 4 ? Double.parseDouble(a[4]) : 20;
        double force = a.length > 5 ? Double.parseDouble(a[5]) : 0;
        W = cx1 - cx0; H = cy1 - cy0; px = new int[H][W];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) px[y][x] = img.getRGB(x + cx0, y + cy0) & 0xFFFFFF;
        double[] gx = findGrid(true, force), gy = findGrid(false, force);
        double p = (gx[0] + gy[0]) / 2;
        System.out.printf(Locale.ROOT, "cell x=%.3f y=%.3f phase x=%.2f y=%.2f%n", gx[0], gy[0], gx[1], gy[1]);
        int gw = (int) Math.floor((W - gx[1]) / p), gh = (int) Math.floor((H - gy[1]) / p);
        int[][] g = new int[gh][gw];
        for (int j = 0; j < gh; j++) for (int i = 0; i < gw; i++) {
            double x0 = gx[1] + i * p, y0 = gy[1] + j * p;
            List<int[]> cols = new ArrayList<>();
            for (int y = (int) (y0 + p * 0.3); y <= (int) (y0 + p * 0.7); y++) for (int x = (int) (x0 + p * 0.3); x <= (int) (x0 + p * 0.7); x++) {
                if (x < 0 || y < 0 || x >= W || y >= H) continue;
                int c = px[y][x]; cols.add(new int[]{(c >> 16) & 255, (c >> 8) & 255, c & 255});
            }
            int[] m = new int[3];
            for (int k = 0; k < 3; k++) { final int kk = k; int[] v = cols.stream().mapToInt(c -> c[kk]).sorted().toArray(); m[k] = v.length == 0 ? 0 : v[v.length / 2]; }
            g[j][i] = (m[0] << 16) | (m[1] << 8) | m[2];
        }
        // background: flood fill from the border through cells close to the border's most common colour
        Map<Integer, Integer> cnt = new HashMap<>();
        for (int i = 0; i < gw; i++) { cnt.merge(g[0][i], 1, Integer::sum); cnt.merge(g[gh - 1][i], 1, Integer::sum); }
        for (int j = 0; j < gh; j++) { cnt.merge(g[j][0], 1, Integer::sum); cnt.merge(g[j][gw - 1], 1, Integer::sum); }
        int bg = Collections.max(cnt.entrySet(), Map.Entry.comparingByValue()).getKey();
        boolean[][] clear = new boolean[gh][gw];
        Deque<int[]> q = new ArrayDeque<>();
        for (int i = 0; i < gw; i++) { q.add(new int[]{i, 0}); q.add(new int[]{i, gh - 1}); }
        for (int j = 0; j < gh; j++) { q.add(new int[]{0, j}); q.add(new int[]{gw - 1, j}); }
        while (!q.isEmpty()) {
            int[] c = q.poll(); int i = c[0], j = c[1];
            if (i < 0 || j < 0 || i >= gw || j >= gh || clear[j][i] || dist(g[j][i], bg) > bgTol) continue;
            clear[j][i] = true;
            q.add(new int[]{i + 1, j}); q.add(new int[]{i - 1, j}); q.add(new int[]{i, j + 1}); q.add(new int[]{i, j - 1});
        }
        // palette cleanup: merge each colour into a more common one within mergeTol
        Map<Integer, Integer> freq = new HashMap<>();
        for (int j = 0; j < gh; j++) for (int i = 0; i < gw; i++) if (!clear[j][i]) freq.merge(g[j][i], 1, Integer::sum);
        List<Integer> order = new ArrayList<>(freq.keySet());
        order.sort((x, y) -> freq.get(y) - freq.get(x));
        List<Integer> pal = new ArrayList<>();
        Map<Integer, Integer> map = new HashMap<>();
        for (int c : order) {
            Integer hit = null;
            for (int pc : pal) if (dist(c, pc) <= mergeTol) { hit = pc; break; }
            if (hit == null) { pal.add(c); hit = c; }
            map.put(c, hit);
        }
        BufferedImage out = new BufferedImage(gw, gh, BufferedImage.TYPE_INT_ARGB);
        for (int j = 0; j < gh; j++) for (int i = 0; i < gw; i++) out.setRGB(i, j, clear[j][i] ? 0 : 0xFF000000 | map.get(g[j][i]));
        ImageIO.write(out, "png", new File(a[1]));
        BufferedImage pv = new BufferedImage(gw * 10, gh * 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < gh * 10; y++) for (int x = 0; x < gw * 10; x++) { int c = out.getRGB(x / 10, y / 10); pv.setRGB(x, y, (c >>> 24) == 0 ? (((x / 5 + y / 5) & 1) == 0 ? 0xFF505050 : 0xFF606060) : c); }
        ImageIO.write(pv, "png", new File(a[1].replace(".png", "-preview.png")));
        System.out.println("grid " + gw + "x" + gh + ", bg #" + Integer.toHexString(bg) + ", palette " + pal.size());
    }
}
