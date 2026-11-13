package logistics.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import logistics.app.NetworkService;
import logistics.model.Graph;

/**
 * 最小生成树页：Prim 与 Kruskal 的结果并排对照。
 */
public final class MstPanel extends JPanel {

    private final NetworkService service;

    private final Ui.StatTile primTile;
    private final Ui.StatTile kruskalTile;
    private final Ui.StatTile checkTile;
    private final Ui.StatTile rebuildTile;
    private final JLabel primTime;
    private final JLabel kruskalTime;
    private final EdgeTableModel primModel = new EdgeTableModel();
    private final EdgeTableModel kruskalModel = new EdgeTableModel();

    public MstPanel(NetworkService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        primTile = new Ui.StatTile("Prim 总成本", "—", "O(V²) · 稠密图更优", Theme.PRIMARY);
        kruskalTile = new Ui.StatTile("Kruskal 总成本", "—", "O(E log E) · 稀疏图更优", Theme.TEXT_SOFT);
        checkTile = new Ui.StatTile("一致性校验", "—", "同一张图总权值必然相同", Theme.SUCCESS);
        rebuildTile = new Ui.StatTile("重建耗时", "—", "完全图 + 两棵 MST", Theme.WARNING);
        primTime = Ui.label("—", 10f, Theme.MUTED, Font.PLAIN);
        kruskalTime = Ui.label("—", 10f, Theme.MUTED, Font.PLAIN);

        JPanel tiles = Ui.transparent(new GridLayout(1, 4, 12, 0));
        tiles.add(primTile);
        tiles.add(kruskalTile);
        tiles.add(checkTile);
        tiles.add(rebuildTile);
        tiles.setPreferredSize(new Dimension(0, 96));
        add(tiles, BorderLayout.NORTH);

        JPanel tables = Ui.transparent(new GridLayout(1, 2, 14, 0));
        tables.add(buildTableCard("Prim", "从起点出发，每次选距当前树最近的顶点",
                primTime, primModel, Theme.PRIMARY));
        tables.add(buildTableCard("Kruskal", "边按权值升序，并查集判环，选够 V-1 条为止",
                kruskalTime, kruskalModel, Theme.TEXT_SOFT));
        add(tables, BorderLayout.CENTER);

        Ui.RoundedPanel note = new Ui.RoundedPanel(new BorderLayout(), new Color(0xF7, 0xF8, 0xF6),
                Theme.BORDER, 6);
        note.setBorder(new EmptyBorder(11, 15, 11, 15));
        JLabel noteText = Ui.label("两种算法选中的边可能不同，但总权值相同。"
                + "地图和最短路径使用 Prim 树；耗时取自算法内部计时。",
                10.3f, Theme.MUTED, Font.PLAIN);
        note.add(noteText, BorderLayout.CENTER);
        note.setPreferredSize(new Dimension(0, 44));
        add(note, BorderLayout.SOUTH);
    }

    private JPanel buildTableCard(String algorithm, String description, JLabel timeLabel,
                                  EdgeTableModel model, Color accent) {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 16, 15, 16));

        JPanel header = Ui.transparent(new BorderLayout());
        JPanel title = Ui.transparent();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        JPanel nameRow = Ui.transparent();
        nameRow.setLayout(new BoxLayout(nameRow, BoxLayout.X_AXIS));
        JLabel name = Ui.label(algorithm, 15.5f, Theme.TEXT, Font.BOLD);
        nameRow.add(name);
        nameRow.add(Ui.gap(8, 0));
        nameRow.add(new Ui.BadgeLabel(algorithm.equals("Prim") ? "加顶点" : "加边",
                accent, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 22)));
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.add(nameRow);
        title.add(Ui.gap(0, 3));
        JLabel desc = Ui.label(description, 10.3f, Theme.MUTED, Font.PLAIN);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.add(desc);
        header.add(title, BorderLayout.WEST);
        timeLabel.setHorizontalAlignment(JLabel.RIGHT);
        timeLabel.setFont(Theme.mono(10.2f));
        header.add(timeLabel, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JTable table = new JTable(model);
        Ui.styleTable(table);
        table.setRowHeight(31);
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(210);
        table.getColumnModel().getColumn(2).setPreferredWidth(65);
        JPanel wrap = Ui.transparent(new BorderLayout());
        wrap.setBorder(new EmptyBorder(10, 0, 0, 0));
        wrap.add(Ui.scroll(table), BorderLayout.CENTER);
        card.add(wrap, BorderLayout.CENTER);
        return card;
    }

    public void refresh() {
        var prim = service.getPrimResult();
        var kruskal = service.getKruskalResult();
        primTile.setValue(Ui.weight(prim.getTotalWeight()));
        primTile.setHint(prim.getEdges().size() + " 条边 · " + service.getGraph().vertexCount() + " 个顶点");
        kruskalTile.setValue(Ui.weight(kruskal.getTotalWeight()));
        kruskalTile.setHint(kruskal.getEdges().size() + " 条边 · " + service.getGraph().edgeCount() + " 条候选");

        boolean same = Math.abs(prim.getTotalWeight() - kruskal.getTotalWeight()) < 1e-6;
        checkTile.setValue(same ? "✓ 一致" : "✗ 不一致");
        checkTile.setValueColor(same ? Theme.SUCCESS : Theme.DANGER);
        checkTile.setHint(same ? "两种算法总权值相同" : "请检查算法实现");

        rebuildTile.setValue(Ui.millis(service.getRebuildMillis()));
        rebuildTile.setHint("图 + Prim + Kruskal");

        primTime.setText("本次 " + Ui.millis(prim.getElapsedMillis()));
        kruskalTime.setText("本次 " + Ui.millis(kruskal.getElapsedMillis()));

        Graph graph = service.getGraph();
        primModel.setEdges(toRows(graph, prim.getEdges()));
        kruskalModel.setEdges(toRows(graph, kruskal.getEdges()));
    }

    private List<EdgeRow> toRows(Graph graph, List<Graph.UndirectedEdge> edges) {
        List<EdgeRow> rows = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            Graph.UndirectedEdge edge = edges.get(i);
            rows.add(new EdgeRow(i + 1,
                    graph.city(edge.getU()).getName() + " — " + graph.city(edge.getV()).getName(),
                    String.format(Locale.ROOT, "%.2f", edge.getWeight())));
        }
        return rows;
    }

    private record EdgeRow(int index, String edge, String weight) {
    }

    private static final class EdgeTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"#", "边", "权值"};
        private List<EdgeRow> rows = List.of();

        void setEdges(List<EdgeRow> rows) {
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
            EdgeRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.index;
                case 1 -> row.edge;
                case 2 -> row.weight;
                default -> "";
            };
        }
    }
}
