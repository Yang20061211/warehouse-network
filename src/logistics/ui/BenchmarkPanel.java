package logistics.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import logistics.algo.Benchmark;
import logistics.app.NetworkService;

/**
 * 算法对比：当前图实测 + 不同规模下的 Prim/Kruskal 对比图。
 */
public final class BenchmarkPanel extends JPanel {

    private static final int[] SIZES = {30, 60, 120, 240, 480};

    private final NetworkService service;

    private final Ui.StatTile primTile;
    private final Ui.StatTile kruskalTile;
    private final Ui.StatTile winnerTile;
    private final Ui.StatTile dijkstraTile;

    private final JRadioButton denseRadio = new JRadioButton("稠密图", true);
    private final JRadioButton sparseRadio = new JRadioButton("稀疏图", false);
    private final JComboBox<String> runsCombo = new JComboBox<>(new String[]{"5次", "10次", "20次"});
    private final Ui.PillButton runButton;
    private final JLabel progressLabel = Ui.label("尚未运行", 9.8f, Theme.FAINT, Font.PLAIN);
    private final ScalabilityModel model = new ScalabilityModel();
    private final ChartPanel chart = new ChartPanel();
    private boolean running;

    public BenchmarkPanel(NetworkService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        primTile = new Ui.StatTile("Prim 平均耗时", "—", "O(V²) · 120 次平均", Theme.PRIMARY);
        kruskalTile = new Ui.StatTile("Kruskal 平均耗时", "—", "O(E log E) · 120 次平均", Theme.MUTED);
        winnerTile = new Ui.StatTile("当前图更快", "—", "同一张完全图", Theme.SUCCESS);
        dijkstraTile = new Ui.StatTile("全点对 Dijkstra", "—", "435 组点对 · 20 轮", Theme.WARNING);

        denseRadio.setToolTipText("完全图：任意两个城市之间都有边");
        sparseRadio.setToolTipText("随机连通图，平均度约为 6");
        runButton = new Ui.PillButton("开始测试", Ui.ButtonStyle.PRIMARY);
        add(buildCurrentCard(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildCurrentCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(15, 18, 15, 18));

        JPanel header = Ui.transparent(new BorderLayout());
        header.add(Ui.cardTitle("当前网络实测"), BorderLayout.WEST);

        JPanel actions = Ui.transparent();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        Ui.PillButton remeasure = new Ui.PillButton("重新测量 MST", Ui.ButtonStyle.SECONDARY);
        remeasure.addActionListener(e -> remeasureCurrent());
        Ui.PillButton dijkstra = new Ui.PillButton("测试全部点对", Ui.ButtonStyle.SECONDARY);
        dijkstra.addActionListener(e -> measureDijkstra(dijkstra));
        actions.add(remeasure);
        actions.add(Ui.gap(8, 0));
        actions.add(dijkstra);
        header.add(actions, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel tiles = Ui.transparent(new GridLayout(1, 4, 10, 0));
        tiles.add(primTile);
        tiles.add(kruskalTile);
        tiles.add(winnerTile);
        tiles.add(dijkstraTile);
        tiles.setPreferredSize(new Dimension(0, 92));
        card.add(tiles, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBody() {
        JPanel body = Ui.transparent(new BorderLayout(14, 0));
        body.add(buildScaleCard(), BorderLayout.WEST);
        body.add(buildChartCard(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildScaleCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 16, 14, 16));
        card.setPreferredSize(new Dimension(520, 0));

        JPanel header = Ui.transparent(new BorderLayout(0, 8));
        JPanel titleRow = Ui.transparent(new BorderLayout());
        titleRow.add(Ui.cardTitle("规模扩展测试"), BorderLayout.WEST);
        header.add(titleRow, BorderLayout.NORTH);

        JPanel middle = Ui.transparent();
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        JPanel controls = Ui.transparent();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        styleRadio(denseRadio);
        styleRadio(sparseRadio);
        ButtonGroup density = new ButtonGroup();
        density.add(denseRadio);
        density.add(sparseRadio);
        controls.add(denseRadio);
        controls.add(Ui.gap(10, 0));
        controls.add(sparseRadio);
        controls.add(Ui.gap(10, 0));
        Ui.styleCombo(runsCombo);
        runsCombo.setPreferredSize(new Dimension(92, 34));
        runsCombo.setMinimumSize(new Dimension(92, 34));
        runsCombo.setSelectedIndex(1);
        controls.add(runsCombo);
        controls.add(Ui.gap(8, 0));
        controls.add(runButton);
        controls.add(Ui.gap(10, 0));
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        runButton.addActionListener(e -> runScalability());
        middle.add(controls);
        middle.add(Ui.gap(0, 5));
        JPanel progressRow = Ui.transparent(new BorderLayout());
        progressRow.add(progressLabel, BorderLayout.WEST);
        progressRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        middle.add(progressRow);
        header.add(middle, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        JTable table = new JTable(model);
        Ui.styleTable(table);
        table.setRowHeight(32);
        table.getColumnModel().getColumn(0).setPreferredWidth(58);
        table.getColumnModel().getColumn(1).setPreferredWidth(76);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setPreferredWidth(95);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);
        Ui.alignNumericColumns(table, 0, 1, 2, 3);
        JPanel tableWrap = Ui.transparent(new BorderLayout());
        tableWrap.add(Ui.scroll(table), BorderLayout.CENTER);
        card.add(tableWrap, BorderLayout.CENTER);

        JLabel note = Ui.label("说明：稠密图 E = V(V−1)/2；稀疏图先生成随机生成树保证连通，再补边。",
                9.6f, Theme.FAINT, Font.PLAIN);
        card.add(note, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildChartCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 16, 15, 16));

        JPanel header = Ui.transparent(new BorderLayout());
        header.add(Ui.cardTitle("耗时对比"), BorderLayout.WEST);
        JLabel hint = Ui.label("单位：ms", 9.8f, Theme.FAINT, Font.PLAIN);
        header.add(hint, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel wrap = Ui.transparent(new BorderLayout());
        wrap.setBorder(new EmptyBorder(8, 0, 0, 0));
        wrap.add(chart, BorderLayout.CENTER);
        card.add(wrap, BorderLayout.CENTER);
        return card;
    }

    private void styleRadio(JRadioButton radio) {
        radio.setOpaque(false);
        radio.setFont(Theme.font(Font.PLAIN, 11f));
        radio.setForeground(Theme.MUTED);
        radio.setFocusPainted(true);
        radio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void refresh() {
        remeasureCurrent();
        dijkstraTile.setValue("—");
        dijkstraTile.setHint("435 组点对 · 20 轮");
    }

    private void remeasureCurrent() {
        Benchmark.MstStats stats = Benchmark.measureMst(service.getGraph(), 120);
        primTile.setValue(Ui.millis(stats.primAvgMs));
        primTile.setHint("120 次平均 · 单次 " + Ui.millis(service.getPrimResult().getElapsedMillis()));
        kruskalTile.setValue(Ui.millis(stats.kruskalAvgMs));
        kruskalTile.setHint("120 次平均 · 单次 " + Ui.millis(service.getKruskalResult().getElapsedMillis()));
        double faster = Math.max(stats.primAvgMs, stats.kruskalAvgMs)
                / Math.max(1e-9, Math.min(stats.primAvgMs, stats.kruskalAvgMs));
        winnerTile.setValue(stats.faster().replace(" 更快", ""));
        winnerTile.setHint(stats.faster() + " · " + String.format(Locale.ROOT, "%.1f×", faster));
    }

    private void measureDijkstra(Ui.PillButton source) {
        source.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Benchmark.PathStats stats = Benchmark.measureDijkstraAllPairs(service.getMstGraph(), 20);
            dijkstraTile.setValue(Ui.millis(stats.avgMs));
            dijkstraTile.setHint(stats.queries + " 次查询 · 最慢 " + Ui.millis(stats.maxMs));
        } finally {
            setCursor(Cursor.getDefaultCursor());
            source.setEnabled(true);
        }
    }

    private void runScalability() {
        if (running) {
            return;
        }
        running = true;
        runButton.setEnabled(false);
        progressLabel.setText("正在生成测试图并计时…");
        progressLabel.setForeground(Theme.MUTED);
        boolean dense = denseRadio.isSelected();
        model.setRows(List.of());
        chart.setRows(List.of(), dense);
        int runs = switch (runsCombo.getSelectedIndex()) {
            case 0 -> 5;
            case 2 -> 20;
            default -> 10;
        };

        SwingWorker<List<Benchmark.Row>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Benchmark.Row> doInBackground() {
                return Benchmark.scalability(SIZES, dense, runs, 2026L);
            }

            @Override
            protected void done() {
                try {
                    List<Benchmark.Row> rows = get();
                    model.setRows(rows);
                    chart.setRows(rows, dense);
                    progressLabel.setText("完成：" + rows.size() + " 个规模 · 每组 "
                            + runs + " 次平均 · " + (dense ? "稠密图" : "稀疏图"));
                    progressLabel.setForeground(Theme.SUCCESS);
                } catch (Exception ex) {
                    progressLabel.setText("测试失败：" + ex.getMessage());
                    progressLabel.setForeground(Theme.DANGER);
                } finally {
                    runButton.setEnabled(true);
                    running = false;
                }
            }
        };
        worker.execute();
    }

    /** 截图脚本调用：同步跑一遍，便于截到完整图表。 */
    public void demoBenchmarkSync() {
        denseRadio.setSelected(true);
        List<Benchmark.Row> rows = Benchmark.scalability(SIZES, true, 5, 2026L);
        model.setRows(rows);
        chart.setRows(rows, true);
        progressLabel.setText("完成：" + rows.size() + " 个规模 · 每组 5 次平均 · 稠密图");
        progressLabel.setForeground(Theme.SUCCESS);
        Benchmark.PathStats stats = Benchmark.measureDijkstraAllPairs(service.getMstGraph(), 5);
        dijkstraTile.setValue(Ui.millis(stats.avgMs));
        dijkstraTile.setHint(stats.queries + " 次查询 · 最慢 " + Ui.millis(stats.maxMs));
    }

    private record Row(int vertices, int edges, String prim, String kruskal, String faster) {
    }

    private static final class ScalabilityModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"规模 V", "边数 E", "Prim (ms)", "Kruskal (ms)", "更快"};
        private List<Row> rows = List.of();

        void setRows(List<Benchmark.Row> data) {
            List<Row> mapped = new ArrayList<>();
            for (Benchmark.Row row : data) {
                mapped.add(new Row(row.vertices, row.edges,
                        String.format(Locale.ROOT, "%.4f", row.primAvgMs),
                        String.format(Locale.ROOT, "%.4f", row.kruskalAvgMs),
                        row.faster()));
            }
            rows = mapped;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Row row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.vertices;
                case 1 -> row.edges;
                case 2 -> row.prim;
                case 3 -> row.kruskal;
                case 4 -> row.faster;
                default -> "";
            };
        }
    }

    private static final class ChartPanel extends JPanel {
        private List<Benchmark.Row> rows = List.of();
        private boolean dense = true;

        ChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, 300));
        }

        void setRows(List<Benchmark.Row> rows, boolean dense) {
            this.rows = List.copyOf(rows);
            this.dense = dense;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (w < 80 || h < 80) {
                g2.dispose();
                return;
            }
            if (rows.isEmpty()) {
                g2.setColor(Theme.FAINT);
                g2.setFont(Theme.font(Font.PLAIN, 12f));
                String text = "运行规模测试后，这里显示 Prim / Kruskal 耗时对比";
                g2.drawString(text, (w - g2.getFontMetrics().stringWidth(text)) / 2, h / 2);
                g2.dispose();
                return;
            }
            int left = 52;
            int right = 18;
            int top = 30;
            int bottom = 42;
            int plotW = w - left - right;
            int plotH = h - top - bottom;

            double min = Double.MAX_VALUE;
            double max = 0;
            for (Benchmark.Row row : rows) {
                min = Math.min(min, Math.min(row.primAvgMs, row.kruskalAvgMs));
                max = Math.max(max, Math.max(row.primAvgMs, row.kruskalAvgMs));
            }
            min = Math.max(0.0001, min * 0.6);
            max = Math.max(max * 1.5, min * 10);
            double logMin = Math.log10(min);
            double logMax = Math.log10(max);

            g2.setFont(Theme.font(Font.PLAIN, 11f));
            g2.setStroke(new BasicStroke(1f));
            for (int exp = (int) Math.floor(logMin); exp <= (int) Math.ceil(logMax); exp++) {
                double value = Math.pow(10, exp);
                if (value < min || value > max) {
                    continue;
                }
                int y = top + (int) Math.round((logMax - Math.log10(value)) / (logMax - logMin) * plotH);
                g2.setColor(Theme.BORDER);
                g2.drawLine(left, y, left + plotW, y);
                g2.setColor(Theme.FAINT);
                String label = value >= 1 ? String.format(Locale.ROOT, "%.0f", value)
                        : value >= 0.1 ? String.format(Locale.ROOT, "%.1f", value)
                        : String.format(Locale.ROOT, "%.3f", value);
                g2.drawString(label, left - 8 - g2.getFontMetrics().stringWidth(label), y + 3);
            }
            g2.setColor(Theme.BORDER_STRONG);
            g2.drawLine(left, top + plotH, left + plotW, top + plotH);

            int groupW = plotW / rows.size();
            int barW = Math.min(30, Math.max(10, groupW / 4));
            for (int i = 0; i < rows.size(); i++) {
                Benchmark.Row row = rows.get(i);
                int center = left + groupW * i + groupW / 2;
                drawBar(g2, center - barW - 3, barW, row.primAvgMs,
                        Theme.PRIMARY, top, plotH, logMin, logMax);
                drawBar(g2, center + 3, barW, row.kruskalAvgMs,
                        Theme.MUTED, top, plotH, logMin, logMax);
                g2.setColor(Theme.MUTED);
                g2.setFont(Theme.font(Font.PLAIN, 11f));
                String label = row.vertices + " 城";
                g2.drawString(label, center - g2.getFontMetrics().stringWidth(label) / 2,
                        top + plotH + 17);
                g2.setColor(Theme.FAINT);
                g2.setFont(Theme.font(Font.PLAIN, 11f));
                String edges = row.edges + " 边";
                g2.drawString(edges, center - g2.getFontMetrics().stringWidth(edges) / 2,
                        top + plotH + 30);
            }

            g2.setFont(Theme.font(Font.BOLD, 11f));
            g2.setColor(Theme.PRIMARY);
            g2.fillRect(left, top - 20, 9, 9);
            g2.setColor(Theme.MUTED);
            g2.setFont(Theme.font(Font.PLAIN, 11f));
            g2.drawString("Prim", left + 14, top - 11);
            g2.setColor(Theme.MUTED);
            g2.fillRect(left + 56, top - 20, 9, 9);
            g2.setColor(Theme.MUTED);
            g2.drawString("Kruskal", left + 70, top - 11);
            g2.setColor(Theme.FAINT);
            g2.drawString(dense ? "稠密图" : "稀疏图", left + 136, top - 11);
            g2.dispose();
        }

        private void drawBar(Graphics2D g2, int x, int barWidth, double value,
                             Color color, int top, int plotH, double logMin, double logMax) {
            double ratio = (Math.log10(value) - logMin) / (logMax - logMin);
            int barH = Math.max(2, (int) Math.round(ratio * plotH));
            int y = top + plotH - barH;
            g2.setColor(color);
            g2.fillRoundRect(x, y, barWidth, barH, 4, 4);
        }
    }
}
