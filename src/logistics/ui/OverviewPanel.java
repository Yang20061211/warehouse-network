package logistics.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import logistics.app.NetworkService;
import logistics.model.City;
import logistics.model.Geo;
import logistics.model.Graph;

/**
 * 网络总览：地图 + 实时统计 + 选中城市详情。
 */
public final class OverviewPanel extends JPanel {

    private final NetworkService service;
    private final MapPanel map;
    private final Ui.StatTile cityTile;
    private final Ui.StatTile edgeTile;
    private final Ui.StatTile mstEdgeTile;
    private final Ui.StatTile costTile;
    private final JLabel averageLabel;
    private final JLabel longestLabel;
    private final JLabel selectedName;
    private final JLabel selectedCoords;
    private final JLabel selectedMeta;
    private final JLabel selectedDistance;
    private String selectedCityName;

    public OverviewPanel(NetworkService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout(16, 0));

        map = new MapPanel(service);
        map.setClickHandler(this::onCityClick);

        JPanel mapCard = Ui.card();
        mapCard.setBorder(new EmptyBorder(16, 16, 14, 16));
        JPanel header = Ui.transparent(new BorderLayout());
        header.add(Ui.cardTitle("城市网络拓扑", "城市按经纬度投影，实线表示最小生成树"), BorderLayout.WEST);

        JPanel controls = Ui.transparent();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        JCheckBox fullNetwork = makeCheckBox("完整网络", false);
        JCheckBox labels = makeCheckBox("城市标签", true);
        fullNetwork.addActionListener(e -> map.setShowFullNetwork(fullNetwork.isSelected()));
        labels.addActionListener(e -> map.setShowLabels(labels.isSelected()));
        controls.add(fullNetwork);
        controls.add(Ui.gap(10, 0));
        controls.add(labels);
        header.add(controls, BorderLayout.EAST);
        mapCard.add(header, BorderLayout.NORTH);

        JPanel mapWrap = Ui.transparent(new BorderLayout());
        mapWrap.setBorder(new EmptyBorder(10, 0, 0, 0));
        mapWrap.add(map, BorderLayout.CENTER);
        mapCard.add(mapWrap, BorderLayout.CENTER);
        mapCard.add(buildLegend(), BorderLayout.SOUTH);
        add(mapCard, BorderLayout.CENTER);

        cityTile = new Ui.StatTile("城市节点", "—", "双向循环链表", Theme.PRIMARY);
        edgeTile = new Ui.StatTile("光纤边", "—", "邻接表存储", Theme.TEXT_SOFT);
        mstEdgeTile = new Ui.StatTile("MST 边数", "—", "V − 1", Theme.SUCCESS);
        costTile = new Ui.StatTile("MST 总成本", "—", "距离单位 ≈ ×111 km", Theme.WARNING);
        averageLabel = Ui.label("—", 10.5f, Theme.MUTED, Font.PLAIN);
        longestLabel = Ui.label("—", 10.5f, Theme.MUTED, Font.PLAIN);

        selectedName = Ui.label("未选择", 21f, Theme.TEXT, Font.BOLD);
        selectedName.setFont(Theme.display(Font.BOLD, 21f));
        selectedCoords = Ui.label("点击地图上的城市查看详情", 10.5f, Theme.MUTED, Font.PLAIN);
        selectedMeta = Ui.label("—", 11f, Theme.TEXT_SOFT, Font.PLAIN);
        selectedDistance = Ui.label("—", 11f, Theme.TEXT_SOFT, Font.PLAIN);

        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(310, 0));
        side.add(buildStatsCard());
        side.add(Ui.gap(0, 14));
        side.add(buildSelectedCard());
        side.add(Ui.gap(0, 14));
        side.add(buildTipCard());
        side.add(javax.swing.Box.createVerticalGlue());
        add(side, BorderLayout.EAST);
    }

    private JCheckBox makeCheckBox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setOpaque(false);
        box.setFont(Theme.font(Font.PLAIN, 11.3f));
        box.setForeground(Theme.MUTED);
        box.setFocusPainted(false);
        box.setIconTextGap(6);
        box.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return box;
    }

    private JPanel buildLegend() {
        JPanel legend = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 18, 6));
        legend.add(legendItem(Ui.legendLine(Theme.PRIMARY, 26), "MST 光纤"));
        legend.add(legendItem(Ui.legendLine(Theme.alpha(Theme.FAINT, 120), 26), "完整网络"));
        legend.add(legendItem(Ui.legendLine(Theme.WARNING, 26), "查询路径"));
        legend.add(legendItem(Ui.legendDot(Color.WHITE, Theme.PRIMARY_DARK), "城市节点"));
        return legend;
    }

    private JPanel legendItem(java.awt.Component mark, String text) {
        JPanel item = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.add(mark);
        item.add(Ui.label(text, 10.5f, Theme.MUTED, Font.PLAIN));
        return item;
    }

    private JPanel buildStatsCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 16, 15, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JPanel title = Ui.transparent(new BorderLayout());
        title.add(Ui.cardTitle("网络概况", "随城市数据自动更新"), BorderLayout.WEST);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Ui.gap(0, 13));

        JPanel grid = Ui.transparent(new GridLayout(2, 2, 10, 10));
        grid.add(cityTile);
        grid.add(edgeTile);
        grid.add(mstEdgeTile);
        grid.add(costTile);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 176));
        card.add(grid);
        card.add(Ui.gap(0, 11));
        card.add(row("MST 平均单段", averageLabel));
        card.add(Ui.gap(0, 4));
        card.add(row("最长 MST 单段", longestLabel));
        return card;
    }

    private JPanel buildSelectedCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));

        JPanel title = Ui.cardTitle("城市详情", "点击地图上的节点");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Ui.gap(0, 12));
        selectedName.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(selectedName);
        selectedCoords.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Ui.gap(0, 3));
        card.add(selectedCoords);
        card.add(Ui.gap(0, 12));
        card.add(row("图中位置", selectedMeta));
        card.add(Ui.gap(0, 5));
        card.add(row("到北京距离", selectedDistance));
        return card;
    }

    private JPanel buildTipCard() {
        Ui.RoundedPanel card = new Ui.RoundedPanel(new BorderLayout(), Theme.PRIMARY_SOFT,
                Theme.BORDER_STRONG, 6);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JLabel title = Ui.label("查询说明", 11.5f, Theme.PRIMARY_DARK, Font.BOLD);
        card.add(title, BorderLayout.NORTH);
        JTextArea text = new JTextArea(
                "最短路径默认在 Prim 生成的最小生成树上查询，可在「最短路径」页切换完整网络。");
        text.setEditable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(Theme.font(Font.PLAIN, 10.3f));
        text.setForeground(Theme.PRIMARY_DARK);
        text.setBorder(new EmptyBorder(6, 0, 0, 0));
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel row(String name, JLabel value) {
        JPanel row = Ui.transparent(new BorderLayout());
        JLabel label = Ui.label(name, 10.8f, Theme.MUTED, Font.PLAIN);
        row.add(label, BorderLayout.WEST);
        value.setHorizontalAlignment(JLabel.RIGHT);
        row.add(value, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    // ─────────────────────────── 刷新 ───────────────────────────

    public void refresh() {
        Graph graph = service.getGraph();
        cityTile.setValue(String.valueOf(graph.vertexCount()));
        cityTile.setHint("链表结点 · " + graph.vertexCount() + " 个");
        edgeTile.setValue(String.valueOf(graph.edgeCount()));
        edgeTile.setHint("邻接表 " + graph.directedEdgeCount() + " 个边结点");
        mstEdgeTile.setValue(String.valueOf(service.getPrimResult().getEdges().size()));
        costTile.setValue(Ui.weight(service.getPrimResult().getTotalWeight()));
        costTile.setHint(String.format(Locale.ROOT, "≈ %.0f km",
                service.getPrimResult().getTotalWeight() * Geo.KM_PER_DEGREE));

        double mstSum = 0;
        double mstLongest = 0;
        for (Graph.UndirectedEdge edge : service.getPrimResult().getEdges()) {
            mstSum += edge.getWeight();
            if (edge.getWeight() > mstLongest) {
                mstLongest = edge.getWeight();
            }
        }
        int mstCount = service.getPrimResult().getEdges().size();
        averageLabel.setText(Ui.weight(mstCount == 0 ? 0 : mstSum / mstCount));
        longestLabel.setText(Ui.weight(mstLongest));

        // 选中城市可能因删除而不存在
        if (selectedCityName != null && !service.getCityList().contains(selectedCityName)) {
            selectedCityName = null;
            map.setSelectedIndex(-1);
        }
        if (selectedCityName == null) {
            selectedName.setText("未选择");
            selectedCoords.setText("点击地图上的城市查看详情");
            selectedMeta.setText("—");
            selectedDistance.setText("—");
        } else {
            updateSelected();
        }
        map.repaint();
    }

    private void onCityClick(int index) {
        List<City> cities = service.snapshotCities();
        if (index < 0 || index >= cities.size()) {
            return;
        }
        selectedCityName = cities.get(index).getName();
        map.setSelectedIndex(index);
        updateSelected();
    }

    private void updateSelected() {
        int index = service.getGraph().indexOf(selectedCityName);
        if (index < 0) {
            selectedName.setText(selectedCityName == null ? "未选择" : selectedCityName);
            selectedCoords.setText("该城市已不在网络中");
            selectedMeta.setText("—");
            selectedDistance.setText("—");
            return;
        }
        City city = service.getGraph().city(index);
        selectedName.setText(city.getName());
        selectedCoords.setText(String.format(Locale.ROOT, "%.2f°E  ·  %.2f°N",
                city.getLon(), city.getLat()));
        selectedMeta.setText("第 " + (index + 1) + " 个顶点 · 度 " + service.getGraph().degree(index));
        City beijing = service.getCityList().find("北京");
        if (beijing == null) {
            selectedDistance.setText("—");
        } else {
            selectedDistance.setText(String.format(Locale.ROOT, "%.2f", Geo.euclid(city, beijing)));
        }
    }
}
