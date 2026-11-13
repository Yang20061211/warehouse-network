package logistics.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import logistics.app.NetworkService;
import logistics.algo.PathResult;
import logistics.model.City;

/**
 * 任意两仓最短路径查询：
 * 默认在 Prim 最小生成树的拓扑上跑 Dijkstra，也可切换完整网络做对比。
 */
public final class PathPanel extends JPanel {

    private final NetworkService service;
    private final Consumer<String> status;

    private final JComboBox<String> fromCombo = new JComboBox<>();
    private final JComboBox<String> toCombo = new JComboBox<>();
    private final JRadioButton mstRadio = new JRadioButton("基于 MST（作业要求）", true);
    private final JRadioButton fullRadio = new JRadioButton("完整网络（对比）", false);
    private final JLabel costValue = Ui.label("—", 28f, Theme.PRIMARY, Font.BOLD);
    private final JLabel metaLabel = Ui.label("选择起点和终点后点击查询", 10.5f, Theme.MUTED, Font.PLAIN);
    private final JLabel modeBadge = new Ui.BadgeLabel("MST 拓扑", Theme.PRIMARY_DARK, Theme.PRIMARY_SOFT);
    private final JTextArea routeArea = new JTextArea();
    private final JTextArea modeNote = new JTextArea();
    private final SegmentModel segmentModel = new SegmentModel();
    private final MapPanel map;
    private final JLabel mapHeader = Ui.label("尚未查询", 11f, Theme.MUTED, Font.PLAIN);
    private final JLabel pickHint = Ui.label("也可以直接点击右侧地图选择起点与终点", 10f,
            Theme.FAINT, Font.PLAIN);

    private boolean pickFrom = true;
    private boolean refreshing;

    public PathPanel(NetworkService service, Consumer<String> status) {
        this.service = service;
        this.status = status;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        map = new MapPanel(service);
        map.setClickHandler(this::onMapClick);

        add(buildQueryCard(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildQueryCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 11));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel header = Ui.transparent(new BorderLayout());
        header.add(Ui.cardTitle("任意两仓最短路径", "Dijkstra · 显示路径、分段权值和耗时"),
                BorderLayout.WEST);
        header.add(pickHint, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel controls = Ui.transparent();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.add(label("起点", 10.8f));
        controls.add(Ui.gap(7, 0));
        Ui.styleCombo(fromCombo);
        fromCombo.setPreferredSize(new Dimension(150, 36));
        controls.add(fromCombo);
        controls.add(Ui.gap(8, 0));
        Ui.PillButton swap = new Ui.PillButton("⇄", Ui.ButtonStyle.SECONDARY);
        swap.setToolTipText("交换起点和终点");
        swap.setBorder(new EmptyBorder(7, 12, 7, 12));
        swap.addActionListener(e -> swapEndpoints());
        controls.add(swap);
        controls.add(Ui.gap(8, 0));
        controls.add(label("终点", 10.8f));
        controls.add(Ui.gap(7, 0));
        Ui.styleCombo(toCombo);
        toCombo.setPreferredSize(new Dimension(150, 36));
        controls.add(toCombo);
        controls.add(Ui.gap(16, 0));

        ButtonGroup group = new ButtonGroup();
        group.add(mstRadio);
        group.add(fullRadio);
        styleRadio(mstRadio);
        styleRadio(fullRadio);
        mstRadio.addActionListener(e -> {
            updateModeNote();
            runQuery();
        });
        fullRadio.addActionListener(e -> {
            updateModeNote();
            runQuery();
        });
        JPanel modes = new JPanel(new GridLayout(1, 2, 6, 0));
        modes.setOpaque(false);
        modes.add(mstRadio);
        modes.add(fullRadio);
        modes.setMaximumSize(new Dimension(290, 36));
        controls.add(modes);
        controls.add(javax.swing.Box.createHorizontalGlue());
        Ui.PillButton query = new Ui.PillButton("查询最短路径", Ui.ButtonStyle.PRIMARY);
        query.addActionListener(e -> runQuery());
        controls.add(query);
        card.add(controls, BorderLayout.CENTER);
        return card;
    }

    private JLabel label(String text, float size) {
        return Ui.label(text, size, Theme.MUTED, Font.PLAIN);
    }

    private void styleRadio(JRadioButton radio) {
        radio.setOpaque(false);
        radio.setFont(Theme.font(Font.PLAIN, 11f));
        radio.setForeground(Theme.MUTED);
        radio.setFocusPainted(false);
        radio.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    private JPanel buildBody() {
        JPanel body = Ui.transparent(new BorderLayout(16, 0));
        body.add(buildResultCard(), BorderLayout.WEST);
        body.add(buildMapCard(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildResultCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 14, 18));
        card.setPreferredSize(new Dimension(430, 0));

        JPanel header = Ui.transparent(new BorderLayout());
        JLabel title = Ui.label("查询结果", 15.5f, Theme.TEXT, Font.BOLD);
        header.add(title, BorderLayout.WEST);
        Ui.PillButton clear = new Ui.PillButton("清空", Ui.ButtonStyle.GHOST);
        clear.setFont(Theme.font(Font.PLAIN, 10.5f));
        clear.addActionListener(e -> clearResult());
        header.add(clear, BorderLayout.EAST);

        JPanel top = Ui.transparent();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(header);
        top.add(Ui.gap(0, 12));

        JLabel costCaption = Ui.label("总代价", 10.5f, Theme.MUTED, Font.PLAIN);
        costCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(costCaption);
        top.add(Ui.gap(0, 2));

        JPanel costRow = Ui.transparent();
        costRow.setLayout(new BoxLayout(costRow, BoxLayout.X_AXIS));
        costValue.setFont(Theme.display(Font.BOLD, 28f));
        costValue.setAlignmentY(Component.CENTER_ALIGNMENT);
        costRow.add(costValue);
        costRow.add(Ui.gap(8, 0));
        costRow.add(label("距离单位", 10.3f));
        costRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(costRow);
        top.add(Ui.gap(0, 4));

        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(metaLabel);
        top.add(Ui.gap(0, 7));
        modeBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(modeBadge);
        top.add(Ui.gap(0, 14));

        JLabel routeTitle = Ui.label("完整路径", 10.5f, Theme.MUTED, Font.PLAIN);
        routeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(routeTitle);
        top.add(Ui.gap(0, 5));

        routeArea.setEditable(false);
        routeArea.setOpaque(false);
        routeArea.setLineWrap(true);
        routeArea.setWrapStyleWord(true);
        routeArea.setFont(Theme.font(Font.PLAIN, 12.3f));
        routeArea.setForeground(Theme.TEXT_SOFT);
        routeArea.setRows(3);
        javax.swing.JScrollPane routeScroll = Ui.scroll(routeArea);
        routeScroll.setPreferredSize(new Dimension(0, 76));
        routeScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        routeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        routeScroll.setOpaque(false);
        routeScroll.getViewport().setOpaque(false);
        top.add(routeScroll);
        card.add(top, BorderLayout.NORTH);

        JPanel segments = Ui.transparent(new BorderLayout(0, 5));
        JLabel segTitle = Ui.label("分段明细", 10.5f, Theme.MUTED, Font.PLAIN);
        segments.add(segTitle, BorderLayout.NORTH);
        JTable table = new JTable(segmentModel);
        Ui.styleTable(table);
        table.setRowHeight(29);
        table.getColumnModel().getColumn(0).setPreferredWidth(38);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        segments.add(Ui.scroll(table), BorderLayout.CENTER);
        card.add(segments, BorderLayout.CENTER);

        modeNote.setEditable(false);
        modeNote.setOpaque(false);
        modeNote.setLineWrap(true);
        modeNote.setWrapStyleWord(true);
        modeNote.setFont(Theme.font(Font.PLAIN, 9.7f));
        modeNote.setForeground(Theme.FAINT);
        modeNote.setRows(2);
        card.add(modeNote, BorderLayout.SOUTH);

        updateModeNote();
        return card;
    }

    private JPanel buildMapCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 14, 16));

        JPanel header = Ui.transparent(new BorderLayout());
        header.add(Ui.cardTitle("路径可视化", "橙色为查询路径，绿色为 MST 光纤"), BorderLayout.WEST);
        mapHeader.setHorizontalAlignment(JLabel.RIGHT);
        header.add(mapHeader, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel wrap = Ui.transparent(new BorderLayout());
        wrap.setBorder(new EmptyBorder(10, 0, 6, 0));
        wrap.add(map, BorderLayout.CENTER);
        card.add(wrap, BorderLayout.CENTER);

        JPanel footer = Ui.transparent(new BorderLayout());
        footer.add(Ui.label("点击地图节点：第一次设起点，第二次设终点并自动查询。",
                9.8f, Theme.FAINT, Font.PLAIN), BorderLayout.WEST);
        JPanel legend = Ui.transparent();
        legend.setLayout(new BoxLayout(legend, BoxLayout.X_AXIS));
        legend.add(Ui.legendLine(Theme.SUCCESS, 22));
        legend.add(Ui.gap(4, 0));
        legend.add(Ui.label("起点", 9.5f, Theme.MUTED, Font.PLAIN));
        legend.add(Ui.gap(10, 0));
        legend.add(Ui.legendLine(Theme.DANGER, 22));
        legend.add(Ui.gap(4, 0));
        legend.add(Ui.label("终点", 9.5f, Theme.MUTED, Font.PLAIN));
        footer.add(legend, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private void updateModeNote() {
        if (mstRadio.isSelected()) {
            modeNote.setText("当前基于 Prim 最小生成树的拓扑：树上两点路径唯一，符合作业要求；"
                    + "代价可能高于完整网络的直达最短路径。");
            modeBadge.setText("MST 拓扑");
            modeBadge.setForeground(Theme.PRIMARY_DARK);
        } else {
            modeNote.setText("当前为完整网络对比模式：路径可能使用非 MST 边，只用于说明两种口径的差异。");
            modeBadge.setText("完整网络");
            modeBadge.setForeground(Theme.SUCCESS);
        }
        modeBadge.repaint();
    }

    // ─────────────────────────── 刷新 / 查询 ───────────────────────────

    public void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        try {
            String oldFrom = (String) fromCombo.getSelectedItem();
            String oldTo = (String) toCombo.getSelectedItem();
            List<City> cities = service.snapshotCities();
            fromCombo.removeAllItems();
            toCombo.removeAllItems();
            for (City city : cities) {
                fromCombo.addItem(city.getName());
                toCombo.addItem(city.getName());
            }
            if (oldFrom != null && cities.stream().anyMatch(c -> c.getName().equals(oldFrom))) {
                fromCombo.setSelectedItem(oldFrom);
            } else if (cities.stream().anyMatch(c -> c.getName().equals("北京"))) {
                fromCombo.setSelectedItem("北京");
            } else if (!cities.isEmpty()) {
                fromCombo.setSelectedIndex(0);
            }
            if (oldTo != null && cities.stream().anyMatch(c -> c.getName().equals(oldTo))) {
                toCombo.setSelectedItem(oldTo);
            } else if (cities.stream().anyMatch(c -> c.getName().equals("乌鲁木齐"))) {
                toCombo.setSelectedItem("乌鲁木齐");
            } else if (cities.size() > 1) {
                toCombo.setSelectedIndex(cities.size() - 1);
            }
            clearResult();
        } finally {
            refreshing = false;
        }
    }

    private void swapEndpoints() {
        Object from = fromCombo.getSelectedItem();
        Object to = toCombo.getSelectedItem();
        if (from == null || to == null) {
            return;
        }
        fromCombo.setSelectedItem(to);
        toCombo.setSelectedItem(from);
        if (service.cityCount() >= 2) {
            runQuery();
        }
    }

    private void onMapClick(int index) {
        String name = service.snapshotCities().get(index).getName();
        if (pickFrom) {
            fromCombo.setSelectedItem(name);
            pickFrom = false;
            pickHint.setText("已选择起点「" + name + "」，请点击终点");
            map.setSelectedIndex(index);
        } else {
            toCombo.setSelectedItem(name);
            pickFrom = true;
            pickHint.setText("已选择终点「" + name + "」，正在查询…");
            map.setSelectedIndex(index);
            runQuery();
        }
    }

    private void runQuery() {
        String from = (String) fromCombo.getSelectedItem();
        String to = (String) toCombo.getSelectedItem();
        if (from == null || to == null) {
            return;
        }
        boolean full = fullRadio.isSelected();
        try {
            NetworkService.QueryStats stats = service.queryPath(from, to, full, 250);
            PathResult result = stats.result;
            if (!result.isReachable()) {
                costValue.setText("不可达");
                metaLabel.setText("两个城市在当前拓扑中不连通");
                routeArea.setText("");
                segmentModel.setRows(List.of());
                map.clearPath();
                mapHeader.setText("不可达");
                return;
            }

            costValue.setText(Ui.weight(result.getTotalWeight()));
            metaLabel.setText(String.format(Locale.ROOT,
                    "%d 个城市 · %d 段 · %d 次平均 %s · 最慢 %s",
                    result.getPath().size(), result.getEdgeCount(), stats.runs,
                    Ui.millis(stats.averageMillis), Ui.millis(stats.maxMillis)));

            List<String> names = new ArrayList<>();
            for (int index : result.getPath()) {
                names.add(service.getGraph().city(index).getName());
            }
            routeArea.setText(String.join("  →  ", names));
            routeArea.setCaretPosition(0);

            List<SegmentRow> rows = new ArrayList<>();
            for (int i = 0; i + 1 < result.getPath().size(); i++) {
                int a = result.getPath().get(i);
                int b = result.getPath().get(i + 1);
                rows.add(new SegmentRow(i + 1,
                        service.getGraph().city(a).getName() + " → " + service.getGraph().city(b).getName(),
                        String.format(Locale.ROOT, "%.2f", edgeWeight(a, b))));
            }
            segmentModel.setRows(rows);

            map.setPath(result.getPath());
            map.setFromTo(result.getSource(), result.getTarget());
            mapHeader.setText(from + " → " + to + "  ·  " + Ui.weight(result.getTotalWeight()));
            pickHint.setText("也可以直接点击右侧地图选择起点与终点");
            status.accept(String.format(Locale.ROOT,
                    "已查询 %s → %s：总代价 %s，%d 段，%d 次平均 %s",
                    from, to, Ui.weight(result.getTotalWeight()), result.getEdgeCount(),
                    stats.runs, Ui.millis(stats.averageMillis)));
        } catch (RuntimeException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private double edgeWeight(int a, int b) {
        for (var edge = service.getGraph().firstEdge(a); edge != null; edge = edge.getNext()) {
            if (edge.getTo() == b) {
                return edge.getWeight();
            }
        }
        return 0;
    }

    private void clearResult() {
        costValue.setText("—");
        metaLabel.setText("选择起点和终点后点击查询");
        routeArea.setText("");
        segmentModel.setRows(List.of());
        map.clearPath();
        mapHeader.setText("尚未查询");
        pickFrom = true;
        pickHint.setText("也可以直接点击右侧地图选择起点与终点");
        updateModeNote();
    }

    /** 供截图脚本调用。 */
    public void demoQuery(String from, String to, boolean fullNetwork) {
        fromCombo.setSelectedItem(from);
        toCombo.setSelectedItem(to);
        if (fullNetwork) {
            fullRadio.setSelected(true);
        } else {
            mstRadio.setSelected(true);
        }
        updateModeNote();
        runQuery();
    }

    private record SegmentRow(int index, String segment, String weight) {
    }

    private static final class SegmentModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"段", "路段", "权值"};
        private List<SegmentRow> rows = List.of();

        void setRows(List<SegmentRow> rows) {
            this.rows = List.copyOf(rows);
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
            SegmentRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.index;
                case 1 -> row.segment;
                case 2 -> row.weight;
                default -> "";
            };
        }
    }
}
