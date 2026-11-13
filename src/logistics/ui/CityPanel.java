package logistics.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import logistics.app.NetworkService;
import logistics.model.City;
import logistics.model.Geo;

/**
 * 城市管理（管理员）：双向循环链表的增删改查、按距离排序，
 * 以及每次变更后的“图 + MST”快速重建。
 */
public final class CityPanel extends JPanel {

    private final NetworkService service;
    private final Consumer<String> status;

    private final CityTableModel model = new CityTableModel();
    private final JTable table = new JTable(model);
    private final JComboBox<String> pivotCombo = new JComboBox<>();
    private final JTextArea sortedArea = new JTextArea();
    private final JLabel countBadge = new Ui.BadgeLabel("— 个结点", Theme.PRIMARY_DARK, Theme.PRIMARY_SOFT);
    private final JLabel listStatus = Ui.label("—", 10.3f, Theme.MUTED, Font.PLAIN);
    private Ui.PillButton editButton;
    private Ui.PillButton deleteButton;
    private boolean refreshing;

    public CityPanel(NetworkService service, Consumer<String> status) {
        this.service = service;
        this.status = status;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        Ui.RoundedPanel card = Ui.card();
        card.setBorder(new EmptyBorder(13, 18, 13, 18));
        card.setLayout(new BorderLayout());

        JPanel titles = Ui.transparent();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = Ui.label("城市管理", 17f, Theme.TEXT, Font.BOLD);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(title);
        titles.add(Ui.gap(0, 3));
        JLabel sub = Ui.label("增删改查会立即重建完全图与 MST；数据自动写入 data/cities.txt",
                10.8f, Theme.MUTED, Font.PLAIN);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(sub);
        card.add(titles, BorderLayout.WEST);

        Ui.PillButton addButton = new Ui.PillButton("＋ 新增城市", Ui.ButtonStyle.PRIMARY);
        editButton = new Ui.PillButton("编辑选中", Ui.ButtonStyle.SECONDARY);
        deleteButton = new Ui.PillButton("删除选中", Ui.ButtonStyle.DANGER);
        Ui.PillButton resetButton = new Ui.PillButton("恢复默认 30 城", Ui.ButtonStyle.GHOST);
        addButton.addActionListener(e -> openAddDialog());
        editButton.addActionListener(e -> openEditDialog());
        deleteButton.addActionListener(e -> deleteSelected());
        resetButton.addActionListener(e -> resetDefaults());

        JPanel actions = Ui.transparent();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.add(addButton);
        actions.add(Ui.gap(8, 0));
        actions.add(editButton);
        actions.add(Ui.gap(8, 0));
        actions.add(deleteButton);
        actions.add(Ui.gap(8, 0));
        actions.add(resetButton);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private JPanel buildBody() {
        JPanel body = Ui.transparent(new BorderLayout(16, 0));

        Ui.RoundedPanel tableCard = Ui.card();
        tableCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        tableCard.setLayout(new BorderLayout());

        JPanel header = Ui.transparent(new BorderLayout());
        header.add(Ui.cardTitle("链表结点", "双击一行可以直接编辑"), BorderLayout.WEST);
        header.add(countBadge, BorderLayout.EAST);
        tableCard.add(header, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 11.3f));
        Ui.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(52);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setPreferredWidth(95);
        table.getColumnModel().getColumn(4).setPreferredWidth(55);
        table.getSelectionModel().addListSelectionListener(e -> updateActionButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    openEditDialog();
                }
            }
        });
        JPanel tableWrap = Ui.transparent(new BorderLayout());
        tableWrap.setBorder(new EmptyBorder(10, 0, 4, 0));
        tableWrap.add(Ui.scroll(table), BorderLayout.CENTER);
        tableCard.add(tableWrap, BorderLayout.CENTER);

        JLabel hint = Ui.label("说明：链表按当前顺序保存；「按距离排序」会把链表结点真正重排。",
                10f, Theme.FAINT, Font.PLAIN);
        tableCard.add(hint, BorderLayout.SOUTH);
        body.add(tableCard, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(405, 0));
        side.add(buildSortCard());
        side.add(javax.swing.Box.createVerticalGlue());
        body.add(side, BorderLayout.EAST);
        return body;
    }

    private JPanel buildSortCard() {
        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 16, 14, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel top = Ui.transparent();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel titles = Ui.cardTitle("按距离排序", "链表就地插入排序 · 稳定 · O(n²)");
        titles.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(titles);
        top.add(Ui.gap(0, 12));

        JPanel controls = Ui.transparent(new BorderLayout(8, 0));
        Ui.styleCombo(pivotCombo);
        pivotCombo.setPreferredSize(new Dimension(160, 36));
        JLabel pivotLabel = Ui.label("基准", 10.3f, Theme.MUTED, Font.PLAIN);
        controls.add(pivotLabel, BorderLayout.WEST);
        controls.add(pivotCombo, BorderLayout.CENTER);
        Ui.PillButton sortButton = new Ui.PillButton("排序并输出", Ui.ButtonStyle.PRIMARY);
        sortButton.addActionListener(e -> doSort());
        controls.add(sortButton, BorderLayout.EAST);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(controls);
        card.add(top, BorderLayout.NORTH);

        sortedArea.setEditable(false);
        sortedArea.setFont(Theme.mono(11.2f));
        sortedArea.setBackground(new Color(0xFB, 0xFC, 0xFE));
        sortedArea.setForeground(Theme.TEXT_SOFT);
        sortedArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        sortedArea.setLineWrap(false);
        JPanel areaWrap = Ui.transparent(new BorderLayout());
        areaWrap.setBorder(new EmptyBorder(10, 0, 8, 0));
        areaWrap.add(Ui.scroll(sortedArea), BorderLayout.CENTER);
        card.add(areaWrap, BorderLayout.CENTER);

        JPanel footer = Ui.transparent();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        listStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.add(listStatus);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    // ─────────────────────────── 刷新 ───────────────────────────

    public void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        try {
            String previousPivot = (String) pivotCombo.getSelectedItem();
            List<City> cities = service.snapshotCities();
            List<RowData> rows = new ArrayList<>();
            for (int i = 0; i < cities.size(); i++) {
                City city = cities.get(i);
                int degree = service.getGraph().indexOf(city.getName()) >= 0
                        ? service.getGraph().degree(service.getGraph().indexOf(city.getName())) : 0;
                rows.add(new RowData(i + 1, city, degree));
            }
            model.setRows(rows);
            countBadge.setText(cities.size() + " 个结点");

            pivotCombo.removeAllItems();
            for (City city : cities) {
                pivotCombo.addItem(city.getName());
            }
            if (previousPivot != null && cities.stream().anyMatch(c -> c.getName().equals(previousPivot))) {
                pivotCombo.setSelectedItem(previousPivot);
            } else if (cities.stream().anyMatch(c -> c.getName().equals("北京"))) {
                pivotCombo.setSelectedItem("北京");
            } else if (!cities.isEmpty()) {
                pivotCombo.setSelectedIndex(0);
            }

            listStatus.setText("双向循环结构自检："
                    + (service.getCityList().checkStructure() ? "通过" : "异常")
                    + " · " + cities.size() + " 个结点 · 重建 "
                    + Ui.millis(service.getRebuildMillis()));
            updateSortedPreview();
            updateActionButtons();
        } finally {
            refreshing = false;
        }
    }

    private void updateSortedPreview() {
        String pivotName = (String) pivotCombo.getSelectedItem();
        City pivot = pivotName == null ? null : service.getCityList().find(pivotName);
        if (pivot == null) {
            sortedArea.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "当前链表序列 · 到「%s」的距离%n", pivotName));
        sb.append("序号  城市              距离\n");
        sb.append("────────────────────────────\n");
        List<City> cities = service.snapshotCities();
        for (int i = 0; i < cities.size(); i++) {
            City city = cities.get(i);
            sb.append(String.format(Locale.ROOT, "%3d   %-12s %8.2f%n",
                    i + 1, city.getName(), Geo.euclid(pivot, city)));
        }
        sortedArea.setText(sb.toString());
        sortedArea.setCaretPosition(0);
    }

    private void updateActionButtons() {
        boolean hasSelection = table.getSelectedRow() >= 0;
        if (editButton != null) {
            editButton.setEnabled(hasSelection);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(hasSelection && service.cityCount() > 2);
        }
    }

    private City selectedCity() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= model.getRowCount()) {
            return null;
        }
        return model.row(row).city;
    }

    // ─────────────────────────── 操作 ───────────────────────────

    private void openAddDialog() {
        CityDialog dialog = new CityDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "新增城市", null, city -> {
                    try {
                        service.addCity(city);
                        return null;
                    } catch (RuntimeException ex) {
                        return ex.getMessage();
                    }
                });
        if (dialog.showDialog()) {
            status.accept("城市「" + dialog.savedCityName() + "」已加入，图与 MST 已重建（"
                    + Ui.millis(service.getRebuildMillis()) + "）");
        }
    }

    private void openEditDialog() {
        City city = selectedCity();
        if (city == null) {
            return;
        }
        String oldName = city.getName();
        CityDialog dialog = new CityDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "编辑城市", city, updated -> {
                    try {
                        service.updateCity(oldName, updated);
                        return null;
                    } catch (RuntimeException ex) {
                        return ex.getMessage();
                    }
                });
        if (dialog.showDialog()) {
            status.accept("城市「" + oldName + "」已更新，图与 MST 已重建（"
                    + Ui.millis(service.getRebuildMillis()) + "）");
        }
    }

    private void deleteSelected() {
        City city = selectedCity();
        if (city == null) {
            return;
        }
        boolean ok = Ui.confirm(this, "删除城市",
                "确定删除「" + city.getName() + "」？\n\n"
                        + "删除后将用剩余城市重新构建完全图、MST 与最短路径数据。");
        if (!ok) {
            return;
        }
        try {
            if (service.removeCity(city.getName())) {
                status.accept("城市「" + city.getName() + "」已删除，图与 MST 已重建（"
                        + Ui.millis(service.getRebuildMillis()) + "）");
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void resetDefaults() {
        boolean ok = Ui.confirm(this, "恢复默认数据",
                "将链表恢复为内置的 30 个城市，并覆盖 data/cities.txt。继续吗？");
        if (!ok) {
            return;
        }
        service.resetDefaults();
        status.accept("已恢复内置 30 城，图与 MST 已重建（" + Ui.millis(service.getRebuildMillis()) + "）");
    }

    private void doSort() {
        String pivot = (String) pivotCombo.getSelectedItem();
        if (pivot == null) {
            return;
        }
        service.sortByDistance(pivot);
        status.accept("双向循环链表已按到「" + pivot + "」的距离重排；图与 MST 已重建（"
                + Ui.millis(service.getRebuildMillis()) + "）");
        refresh();
    }

    /** 供截图/演示脚本调用。 */
    public void demoSort(String pivot) {
        pivotCombo.setSelectedItem(pivot);
        doSort();
    }

    // ─────────────────────────── 数据模型 ───────────────────────────

    private record RowData(int index, City city, int degree) {
    }

    private static final class CityTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"序号", "城市", "经度", "纬度", "度"};
        private List<RowData> rows = List.of();

        void setRows(List<RowData> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        RowData row(int index) {
            return rows.get(index);
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
            RowData row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.index;
                case 1 -> row.city.getName();
                case 2 -> String.format(Locale.ROOT, "%.2f", row.city.getLon());
                case 3 -> String.format(Locale.ROOT, "%.2f", row.city.getLat());
                case 4 -> row.degree;
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    // ─────────────────────────── 新增 / 编辑对话框 ───────────────────────────

    @FunctionalInterface
    private interface SaveHandler {
        /** 返回 null 表示保存成功，否则返回给用户看的错误信息。 */
        String save(City city);
    }

    private static final class CityDialog extends JDialog {
        private final JTextField nameField = new JTextField();
        private final JTextField lonField = new JTextField();
        private final JTextField latField = new JTextField();
        private final JLabel errorLabel = new JLabel(" ");
        private final SaveHandler handler;
        private boolean saved;
        private String savedName;

        CityDialog(Window owner, String title, City existing, SaveHandler handler) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            this.handler = handler;
            setResizable(false);
            setSize(470, 430);
            setLocationRelativeTo(owner);
            setContentPane(buildContent(existing));
            getRootPane().setDefaultButton(findSaveButton());
        }

        private Ui.PillButton saveButton;

        private JPanel buildContent(City existing) {
            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Color.WHITE);
            root.setBorder(new EmptyBorder(24, 26, 20, 26));

            JPanel top = Ui.transparent();
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            JLabel title = Ui.label(existing == null ? "新增城市" : "编辑城市", 21f,
                    Theme.TEXT, Font.BOLD);
            title.setFont(Theme.display(Font.BOLD, 21f));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(title);
            top.add(Ui.gap(0, 5));
            JLabel sub = Ui.label("保存后立即重建完全图、MST 与最短路径缓存", 10.5f,
                    Theme.MUTED, Font.PLAIN);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(sub);
            root.add(top, BorderLayout.NORTH);

            JPanel form = new JPanel();
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBorder(new EmptyBorder(18, 0, 0, 0));

            nameField.setText(existing == null ? "" : existing.getName());
            lonField.setText(existing == null ? "" : String.format(Locale.ROOT, "%.2f", existing.getLon()));
            latField.setText(existing == null ? "" : String.format(Locale.ROOT, "%.2f", existing.getLat()));
            Ui.styleField(nameField);
            Ui.styleField(lonField);
            Ui.styleField(latField);

            form.add(field("城市名称", nameField));
            form.add(Ui.gap(0, 12));
            JPanel coords = Ui.transparent(new GridLayout(1, 2, 12, 0));
            coords.add(field("经度（°E）", lonField));
            coords.add(field("纬度（°N）", latField));
            coords.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            coords.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(coords);
            form.add(Ui.gap(0, 8));
            JLabel hint = Ui.label("边权 = 与其它城市的经纬度欧氏距离（保留 2 位小数）",
                    9.8f, Theme.FAINT, Font.PLAIN);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(hint);

            form.add(Ui.gap(0, 4));
            errorLabel.setForeground(Theme.DANGER);
            errorLabel.setFont(Theme.font(Font.PLAIN, 10.5f));
            errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(errorLabel);
            root.add(form, BorderLayout.CENTER);

            JPanel actions = Ui.transparent();
            actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
            actions.add(javax.swing.Box.createHorizontalGlue());
            Ui.PillButton cancel = new Ui.PillButton("取消", Ui.ButtonStyle.SECONDARY);
            saveButton = new Ui.PillButton("保存", Ui.ButtonStyle.PRIMARY);
            cancel.addActionListener(e -> dispose());
            saveButton.addActionListener(e -> save());
            actions.add(cancel);
            actions.add(Ui.gap(10, 0));
            actions.add(saveButton);
            root.add(actions, BorderLayout.SOUTH);
            return root;
        }

        private Ui.PillButton findSaveButton() {
            return saveButton;
        }

        private JPanel field(String labelText, JTextField field) {
            JPanel panel = Ui.transparent();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            JLabel label = Ui.label(labelText, 10.5f, Theme.TEXT_SOFT, Font.BOLD);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
            panel.add(Ui.gap(0, 6));
            field.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(field);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            return panel;
        }

        private void save() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                errorLabel.setText("请填写城市名称");
                return;
            }
            double lon;
            double lat;
            try {
                lon = Double.parseDouble(lonField.getText().trim());
                lat = Double.parseDouble(latField.getText().trim());
            } catch (NumberFormatException ex) {
                errorLabel.setText("经度和纬度必须是数字");
                return;
            }
            if (!Double.isFinite(lon) || !Double.isFinite(lat)
                    || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
                errorLabel.setText("坐标超出范围：经度 -180~180，纬度 -90~90");
                return;
            }
            City city = new City(name, lon, lat);
            String error = handler.save(city);
            if (error != null) {
                errorLabel.setText(error);
                return;
            }
            saved = true;
            savedName = name;
            dispose();
        }

        boolean showDialog() {
            setVisible(true);
            return saved;
        }

        String savedCityName() {
            return savedName;
        }
    }
}
