package logistics.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JPanel;
import logistics.app.NetworkService;
import logistics.model.City;
import logistics.model.Graph;

/**
 * 城市网络地图（自绘）。
 *
 * <p>用经纬度投影把 30 个城市画在一张示意中国地图上：
 * 绿色实线是最小生成树，灰色细线是完整网络，橙色为最短路径。
 * 标注采用简单的碰撞规避，尽量不重叠。</p>
 */
public final class MapPanel extends JPanel {

    private static final double MIN_LON = 72.0;
    private static final double MAX_LON = 136.0;
    private static final double MIN_LAT = 17.0;
    private static final double MAX_LAT = 55.0;
    /** 中纬度经度方向的压缩系数，让地图比例更接近真实。 */
    private static final double LON_FACTOR = Math.cos(Math.toRadians(35));

    private static final Color OCEAN = new Color(0xF7, 0xF9, 0xF6);
    private static final Color LAND = new Color(0xF4, 0xF2, 0xEA);
    private static final Color LAND_EDGE = new Color(0xD5, 0xDB, 0xD5);
    private static final Color GRID = new Color(0xDD, 0xE2, 0xDD);
    private static final Color MST = Theme.PRIMARY;
    private static final Color NODE = Theme.PRIMARY_DARK;
    private static final Color PATH = Theme.WARNING;
    private static final Color START = Theme.SUCCESS;
    private static final Color END = Theme.DANGER;

    private final NetworkService service;

    private boolean showFullNetwork;
    private boolean showLabels = true;
    private List<Integer> path = List.of();
    private int from = -1;
    private int to = -1;
    private int selected = -1;
    private int hover = -1;
    private int mouseX;
    private int mouseY;

    private double scale = 1;
    private double offsetX;
    private double offsetY;

    private Consumer<Integer> clickListener;

    public MapPanel(NetworkService service) {
        this.service = service;
        setOpaque(false);
        setCursor(Cursor.getDefaultCursor());
        setToolTipText("");

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                int near = nearestCity(e.getX(), e.getY());
                if (near != hover) {
                    hover = near;
                }
                setCursor(near >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int index = nearestCity(e.getX(), e.getY());
                if (index >= 0) {
                    selected = index;
                    if (clickListener != null) {
                        clickListener.accept(index);
                    }
                    repaint();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // ─────────────────────────── 对外状态 ───────────────────────────

    public void setClickHandler(Consumer<Integer> clickListener) {
        this.clickListener = clickListener;
    }

    public void setShowFullNetwork(boolean value) {
        this.showFullNetwork = value;
        repaint();
    }

    public void setShowLabels(boolean value) {
        this.showLabels = value;
        repaint();
    }

    public void setPath(List<Integer> path) {
        this.path = path == null ? List.of() : List.copyOf(path);
        repaint();
    }

    public void setFromTo(int from, int to) {
        this.from = from;
        this.to = to;
        repaint();
    }

    public void clearPath() {
        path = List.of();
        from = -1;
        to = -1;
        selected = -1;
        repaint();
    }

    public void setSelectedIndex(int index) {
        selected = index;
        repaint();
    }

    public int getSelectedIndex() {
        return selected;
    }

    // ─────────────────────────── 绘制 ───────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.clip(new RoundRectangle2D.Double(0, 0, w, h, 8, 8));

        g2.setColor(OCEAN);
        g2.fillRect(0, 0, w, h);

        updateTransform();
        drawGraticule(g2, w, h);
        drawLand(g2, w, h);
        drawEdges(g2);
        drawPath(g2);
        drawNodesAndLabels(g2, w, h);
        drawCompass(g2, w, h);
        drawScaleBar(g2, w, h);
        drawHover(g2, w, h);

        g2.dispose();
    }

    private void updateTransform() {
        double pad = 26;
        double availW = Math.max(20, getWidth() - pad * 2);
        double availH = Math.max(20, getHeight() - pad * 2);
        double spanX = (MAX_LON - MIN_LON) * LON_FACTOR;
        double spanY = MAX_LAT - MIN_LAT;
        scale = Math.min(availW / spanX, availH / spanY);
        offsetX = (getWidth() - spanX * scale) / 2.0;
        offsetY = (getHeight() - spanY * scale) / 2.0;
    }

    private int xOf(double lon) {
        return (int) Math.round(offsetX + (lon - MIN_LON) * LON_FACTOR * scale);
    }

    private int yOf(double lat) {
        return (int) Math.round(offsetY + (MAX_LAT - lat) * scale);
    }

    private void drawGraticule(Graphics2D g2, int w, int h) {
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(Theme.font(Font.PLAIN, 9f));
        g2.setColor(Theme.alpha(Theme.MUTED, 70));
        for (double lon = 75; lon <= 135; lon += 5) {
            int x = xOf(lon);
            g2.setColor(GRID);
            g2.drawLine(x, yOf(MAX_LAT), x, yOf(MIN_LAT));
            if (((int) lon) % 15 == 0) {
                g2.setColor(Theme.alpha(Theme.MUTED, 120));
                String text = (int) lon + "°E";
                g2.drawString(text, x - g2.getFontMetrics().stringWidth(text) / 2,
                        yOf(MIN_LAT) + 18);
            }
        }
        for (double lat = 20; lat <= 55; lat += 5) {
            int y = yOf(lat);
            g2.setColor(GRID);
            g2.drawLine(xOf(MIN_LON), y, xOf(MAX_LON), y);
            if (((int) lat) % 10 == 0) {
                g2.setColor(Theme.alpha(Theme.MUTED, 120));
                int labelX = Math.max(4, xOf(MIN_LON) - 34);
                g2.drawString((int) lat + "°N", labelX, y + 4);
            }
        }
        g2.setColor(Theme.alpha(Theme.MUTED, 90));
        g2.drawString("黄海", xOf(123.5), yOf(35.0));
        g2.drawString("东海", xOf(125.0), yOf(28.0));
        g2.drawString("南海", xOf(113.0), yOf(19.0));
    }

    private void drawLand(Graphics2D g2, int w, int h) {
        Path2D.Double outline = new Path2D.Double();
        double[][] pts = CHINA_OUTLINE;
        for (int i = 0; i < pts.length; i++) {
            double x = xOf(pts[i][0]);
            double y = yOf(pts[i][1]);
            if (i == 0) {
                outline.moveTo(x, y);
            } else {
                outline.lineTo(x, y);
            }
        }
        outline.closePath();
        g2.setColor(LAND);
        g2.fill(outline);
        g2.setColor(LAND_EDGE);
        g2.setStroke(new BasicStroke(1.15f));
        g2.draw(outline);

        // 海南岛
        double hainanW = 2.4 * LON_FACTOR * scale;
        double hainanH = 1.15 * scale;
        Ellipse2D hainan = new Ellipse2D.Double(xOf(110.3) - hainanW / 2,
                yOf(19.2) - hainanH / 2, hainanW, hainanH);
        g2.setColor(LAND);
        g2.fill(hainan);
        g2.setColor(LAND_EDGE);
        g2.draw(hainan);

        // 台湾岛
        double taiwanW = 0.75 * LON_FACTOR * scale;
        double taiwanH = 1.9 * scale;
        Ellipse2D taiwan = new Ellipse2D.Double(xOf(121.0) - taiwanW / 2,
                yOf(23.7) - taiwanH / 2, taiwanW, taiwanH);
        g2.setColor(LAND);
        g2.fill(taiwan);
        g2.setColor(LAND_EDGE);
        g2.draw(taiwan);

        // 轻微的图幅边框
        g2.setColor(Theme.alpha(Theme.BORDER_STRONG, 160));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(xOf(MIN_LON) - 8, yOf(MAX_LAT) - 8,
                (MAX_LON - MIN_LON) * LON_FACTOR * scale + 16,
                (MAX_LAT - MIN_LAT) * scale + 16, 12, 12));
    }

    private void drawEdges(Graphics2D g2) {
        Graph graph = service.getGraph();
        if (showFullNetwork) {
            g2.setColor(Theme.alpha(Theme.FAINT, 52));
            g2.setStroke(new BasicStroke(0.75f));
            for (Graph.UndirectedEdge edge : graph.edges()) {
                g2.drawLine(xOf(graph.city(edge.getU()).getLon()),
                        yOf(graph.city(edge.getU()).getLat()),
                        xOf(graph.city(edge.getV()).getLon()),
                        yOf(graph.city(edge.getV()).getLat()));
            }
        }

        // MST 先用白色描边，增强在底图上的可读性
        g2.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 255, 210));
        for (Graph.UndirectedEdge edge : service.getPrimResult().getEdges()) {
            g2.drawLine(xOf(graph.city(edge.getU()).getLon()), yOf(graph.city(edge.getU()).getLat()),
                    xOf(graph.city(edge.getV()).getLon()), yOf(graph.city(edge.getV()).getLat()));
        }
        g2.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.alpha(MST, 220));
        for (Graph.UndirectedEdge edge : service.getPrimResult().getEdges()) {
            g2.drawLine(xOf(graph.city(edge.getU()).getLon()), yOf(graph.city(edge.getU()).getLat()),
                    xOf(graph.city(edge.getV()).getLon()), yOf(graph.city(edge.getV()).getLat()));
        }
    }

    private void drawPath(Graphics2D g2) {
        if (path.size() < 2) {
            return;
        }
        Graph graph = service.getGraph();
        // 浅色外沿 + 橙色主线
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.alpha(PATH, 45));
        drawPathSegments(g2, graph);
        g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(PATH);
        drawPathSegments(g2, graph);
    }

    private void drawPathSegments(Graphics2D g2, Graph graph) {
        for (int i = 0; i + 1 < path.size(); i++) {
            int a = path.get(i);
            int b = path.get(i + 1);
            if (a < 0 || b < 0 || a >= graph.vertexCount() || b >= graph.vertexCount()) {
                continue;
            }
            g2.drawLine(xOf(graph.city(a).getLon()), yOf(graph.city(a).getLat()),
                    xOf(graph.city(b).getLon()), yOf(graph.city(b).getLat()));
        }
    }

    private void drawNodesAndLabels(Graphics2D g2, int w, int h) {
        List<City> cities = service.snapshotCities();
        if (cities.isEmpty()) {
            g2.setColor(Theme.MUTED);
            g2.setFont(Theme.font(Font.PLAIN, 14f));
            g2.drawString("暂无城市数据", w / 2 - 45, h / 2);
            return;
        }
        Set<Integer> pathSet = new HashSet<>(path);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < cities.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingInt(i -> priority(i, pathSet)));

        // 城市点
        for (int i = 0; i < cities.size(); i++) {
            City city = cities.get(i);
            int x = xOf(city.getLon());
            int y = yOf(city.getLat());
            boolean isPath = pathSet.contains(i);
            boolean isStart = i == from;
            boolean isEnd = i == to;
            boolean isHot = i == selected || i == hover;

            if (isHot) {
                g2.setColor(Theme.alpha(NODE, 30));
                g2.fillOval(x - 11, y - 11, 22, 22);
            }
            if (isStart || isEnd) {
                Color ring = isStart ? START : END;
                g2.setColor(Theme.alpha(ring, 45));
                g2.fillOval(x - 12, y - 12, 24, 24);
                g2.setColor(Color.WHITE);
                g2.fillOval(x - 7, y - 7, 14, 14);
                g2.setColor(ring);
                g2.setStroke(new BasicStroke(2.3f));
                g2.drawOval(x - 7, y - 7, 14, 14);
                g2.fillOval(x - 3, y - 3, 6, 6);
            } else {
                int r = isPath ? 5 : 4;
                g2.setColor(Color.WHITE);
                g2.fillOval(x - r, y - r, r * 2, r * 2);
                g2.setColor(isPath ? PATH : NODE);
                g2.setStroke(new BasicStroke(isPath ? 2f : 1.7f));
                g2.drawOval(x - r, y - r, r * 2, r * 2);
                if (isHot) {
                    g2.setColor(isPath ? PATH : NODE);
                    g2.fillOval(x - 2, y - 2, 4, 4);
                }
            }
        }

        if (!showLabels) {
            return;
        }

        // 标签：优先画路径点、起终点，再做碰撞检测
        Font font = Theme.font(Font.PLAIN, 10.4f);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        List<Rectangle> placed = new ArrayList<>();
        List<Rectangle> dots = new ArrayList<>();
        for (int i = 0; i < cities.size(); i++) {
            int x = xOf(cities.get(i).getLon());
            int y = yOf(cities.get(i).getLat());
            dots.add(new Rectangle(x - 7, y - 7, 14, 14));
        }

        for (int i : order) {
            City city = cities.get(i);
            int x = xOf(city.getLon());
            int y = yOf(city.getLat());
            int tw = fm.stringWidth(city.getName());
            int th = fm.getHeight();
            int[][] cand = {
                    {x + 9, y - th / 2},
                    {x - tw - 9, y - th / 2},
                    {x - tw / 2, y - th - 8},
                    {x - tw / 2, y + 9},
                    {x + 8, y - th - 4},
                    {x - tw - 8, y - th - 4},
                    {x + 8, y + 4},
                    {x - tw - 8, y + 4}
            };
            Rectangle chosen = null;
            for (int[] c : cand) {
                Rectangle rect = new Rectangle(c[0], c[1], tw, th);
                if (rect.x < 4 || rect.y < 4 || rect.x + rect.width > w - 4
                        || rect.y + rect.height > h - 4) {
                    continue;
                }
                boolean clash = false;
                for (Rectangle other : placed) {
                    if (other.intersects(rect)) {
                        clash = true;
                        break;
                    }
                }
                if (!clash) {
                    for (Rectangle dot : dots) {
                        if (dot.intersects(rect)) {
                            clash = true;
                            break;
                        }
                    }
                }
                if (!clash) {
                    chosen = rect;
                    break;
                }
            }
            if (chosen == null) {
                continue;
            }
            placed.add(chosen);

            boolean isPath = pathSet.contains(i);
            Color color = i == from ? START
                    : i == to ? END
                    : isPath ? PATH
                    : i == hover || i == selected ? Theme.PRIMARY_DARK
                    : Theme.TEXT_SOFT;
            int baseline = chosen.y + fm.getAscent();
            g2.setColor(new Color(255, 255, 255, 235));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    g2.drawString(city.getName(), chosen.x + dx, baseline + dy);
                }
            }
            g2.setColor(color);
            g2.drawString(city.getName(), chosen.x, baseline);
        }
    }

    private int priority(int index, Set<Integer> pathSet) {
        if (index == from || index == to) {
            return 0;
        }
        if (pathSet.contains(index)) {
            return 1;
        }
        if (index == selected || index == hover) {
            return 2;
        }
        return 3;
    }

    private void drawCompass(Graphics2D g2, int w, int h) {
        int x = w - 34;
        int y = 34;
        g2.setColor(Theme.alpha(Theme.MUTED, 150));
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y + 9, x, y - 9);
        g2.drawLine(x, y - 9, x - 4, y - 3);
        g2.drawLine(x, y - 9, x + 4, y - 3);
        g2.setFont(Theme.font(Font.BOLD, 9.5f));
        g2.drawString("N", x - 4, y + 21);
    }

    private void drawScaleBar(Graphics2D g2, int w, int h) {
        double degrees = 500.0 / 111.0;
        double px = degrees * scale;
        int x2 = w - 28;
        int x1 = (int) Math.round(x2 - px);
        int y = h - 26;
        g2.setColor(Theme.alpha(Theme.MUTED, 130));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawLine(x1, y, x2, y);
        g2.drawLine(x1, y - 4, x1, y + 4);
        g2.drawLine(x2, y - 4, x2, y + 4);
        g2.setFont(Theme.font(Font.PLAIN, 9.5f));
        String text = "≈ 500 km";
        g2.drawString(text, x1 + (int) ((px - g2.getFontMetrics().stringWidth(text)) / 2), y - 7);
        g2.setColor(Theme.alpha(Theme.FAINT, 120));
        g2.drawString("示意图 · 按经纬度投影", 22, h - 24);
    }

    private void drawHover(Graphics2D g2, int w, int h) {
        List<City> cities = service.snapshotCities();
        if (hover < 0 || hover >= cities.size() || !isShowing()) {
            return;
        }
        City city = cities.get(hover);
        int index = service.getGraph().indexOf(city.getName());
        String text = city.getName() + "  " + String.format(java.util.Locale.ROOT,
                "%.2f°E, %.2f°N", city.getLon(), city.getLat());
        String sub = index >= 0 ? "度 " + service.getGraph().degree(index) + " · 点击选定" : "";
        g2.setFont(Theme.font(Font.PLAIN, 11f));
        FontMetrics fm = g2.getFontMetrics();
        int tw = Math.max(fm.stringWidth(text), fm.stringWidth(sub)) + 22;
        int th = 44;
        int x = Math.min(w - tw - 10, mouseX + 14);
        int y = Math.min(h - th - 10, mouseY + 16);
        g2.setColor(Theme.alpha(Theme.TEXT, 235));
        g2.fillRoundRect(x, y, tw, th, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x + 11, y + 18);
        g2.setColor(new Color(0xD9, 0xDE, 0xD8));
        g2.setFont(Theme.font(Font.PLAIN, 9.5f));
        g2.drawString(sub, x + 11, y + 33);
    }

    private int nearestCity(int px, int py) {
        List<City> cities = service.snapshotCities();
        int best = -1;
        double bestDist = 14 * 14;
        for (int i = 0; i < cities.size(); i++) {
            double dx = xOf(cities.get(i).getLon()) - px;
            double dy = yOf(cities.get(i).getLat()) - py;
            double d = dx * dx + dy * dy;
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private static final class Rectangle {
        final int x;
        final int y;
        final int width;
        final int height;

        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean intersects(Rectangle other) {
            return x < other.x + other.width && other.x < x + width
                    && y < other.y + other.height && other.y < y + height;
        }
    }

    /** 简化中国轮廓（经度, 纬度），仅用于示意。 */
    private static final double[][] CHINA_OUTLINE = {
            {73.5, 39.5}, {74.5, 40.5}, {75.0, 41.5}, {76.5, 43.0},
            {80.2, 45.0}, {82.5, 45.2}, {83.0, 47.2}, {85.5, 48.0},
            {87.3, 49.2}, {90.0, 49.0}, {92.5, 50.0}, {95.5, 49.5},
            {97.5, 49.2}, {99.0, 50.5}, {102.0, 51.5}, {106.0, 50.5},
            {108.0, 49.5}, {111.0, 49.8}, {114.5, 50.2}, {117.0, 49.5},
            {119.8, 50.2}, {120.5, 52.5}, {122.5, 53.5}, {125.5, 53.0},
            {127.5, 50.5}, {130.8, 48.8}, {133.0, 48.2}, {134.7, 48.4},
            {134.0, 47.0}, {133.0, 45.2}, {131.3, 45.0}, {131.0, 43.2},
            {130.6, 42.8}, {129.5, 42.5}, {128.0, 41.5}, {125.5, 40.8},
            {124.3, 39.8}, {122.0, 39.2}, {121.0, 38.0}, {120.0, 37.3},
            {122.0, 37.0}, {121.5, 36.0}, {120.5, 34.0}, {121.5, 31.9},
            {121.9, 30.8}, {121.0, 29.0}, {121.5, 28.0}, {119.8, 25.7},
            {118.5, 24.5}, {117.0, 23.5}, {115.0, 22.7}, {113.0, 22.0},
            {111.0, 21.5}, {110.0, 20.2}, {109.5, 18.2}, {108.0, 21.5},
            {106.7, 22.0}, {105.5, 23.2}, {103.5, 22.5}, {101.8, 22.0},
            {100.0, 21.5}, {99.0, 23.0}, {98.0, 24.5}, {97.5, 28.0},
            {95.0, 29.0}, {91.5, 27.5}, {89.0, 27.8}, {88.0, 28.0},
            {85.0, 28.0}, {82.0, 30.0}, {80.0, 32.0}, {78.0, 35.0},
            {76.0, 35.5}, {74.5, 37.0}
    };
}
