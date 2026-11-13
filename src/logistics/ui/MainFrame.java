package logistics.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import logistics.app.NetworkService;
import logistics.app.Role;
import logistics.app.User;

/**
 * 主窗口：左侧导航 + 右侧功能页 + 顶部身份区 + 底部状态栏。
 * 管理员与查询员看到不同的导航与操作权限。
 */
public final class MainFrame extends JFrame {

    private final NetworkService service;
    private final User user;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final Map<String, Ui.NavButton> navButtons = new LinkedHashMap<>();
    private final JLabel sidebarStats = Ui.label("—", 10.5f, Theme.TEXT_SOFT, Font.PLAIN);
    private final JLabel sidebarCost = Ui.label("—", 10.5f, Theme.TEXT_SOFT, Font.PLAIN);
    private final JLabel statusLeft = Ui.label("请选择左侧功能", 10.5f, Theme.MUTED, Font.PLAIN);
    private final JLabel statusRight = Ui.label("—", 10.5f, Theme.MUTED, Font.PLAIN);

    private OverviewPanel overviewPanel;
    private CityPanel cityPanel;
    private AdjacencyPanel adjacencyPanel;
    private MstPanel mstPanel;
    private PathPanel pathPanel;
    private BenchmarkPanel benchmarkPanel;
    private AboutPanel aboutPanel;

    private boolean refreshing;

    public MainFrame(NetworkService service, User user) {
        super("城际仓网规划系统 · " + user.getRole().getLabel());
        this.service = service;
        this.user = user;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 860);
        setMinimumSize(new Dimension(1180, 740));
        setLocationRelativeTo(null);
        setContentPane(buildRoot());
        service.addListener(this::refreshAll);
        refreshAll();
        showCard("overview");
    }

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildStatus(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(11, 20, 11, 22)));
        header.setPreferredSize(new Dimension(0, 62));

        JPanel left = Ui.transparent();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        JLabel title = Ui.label("城际仓网规划系统", 16.5f, Theme.TEXT, Font.BOLD);
        title.setAlignmentY(Component.CENTER_ALIGNMENT);
        left.add(title);
        header.add(left, BorderLayout.WEST);

        JPanel right = Ui.transparent();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        boolean admin = user.getRole() == Role.ADMIN;
        Ui.BadgeLabel roleBadge = new Ui.BadgeLabel(user.getRole().getLabel(),
                admin ? Theme.PRIMARY_DARK : Theme.SUCCESS,
                admin ? Theme.PRIMARY_SOFT : Theme.SUCCESS_SOFT);
        roleBadge.setAlignmentY(Component.CENTER_ALIGNMENT);
        right.add(roleBadge);
        right.add(Ui.gap(10, 0));
        JLabel username = Ui.label(user.getDisplayName() + " · " + user.getUsername(),
                10.5f, Theme.MUTED, Font.PLAIN);
        username.setAlignmentY(Component.CENTER_ALIGNMENT);
        right.add(username);
        right.add(Ui.gap(14, 0));
        Ui.PillButton logout = new Ui.PillButton("退出登录", Ui.ButtonStyle.GHOST);
        logout.setIcon(Icons.of(Icons.Type.LOGOUT, Theme.MUTED));
        logout.setIconTextGap(6);
        logout.setFont(Theme.font(Font.PLAIN, 11.3f));
        logout.setAlignmentY(Component.CENTER_ALIGNMENT);
        logout.addActionListener(e -> logout());
        right.add(logout);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(214, 0));
        sidebar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, Theme.BORDER),
                new EmptyBorder(18, 12, 16, 12)));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel section = Ui.label("功能", 10f, Theme.SIDEBAR_MUTED, Font.BOLD);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(new EmptyBorder(0, 10, 0, 0));
        sidebar.add(section);
        sidebar.add(Ui.gap(0, 10));

        addNav(sidebar, "overview", "网络总览", Icons.Type.MAP);
        if (user.getRole().canEdit()) {
            addNav(sidebar, "cities", "城市管理", Icons.Type.CITY);
        }
        addNav(sidebar, "adjacency", "邻接表", Icons.Type.LIST);
        addNav(sidebar, "mst", "最小生成树", Icons.Type.TREE);
        addNav(sidebar, "path", "最短路径", Icons.Type.PATH);
        if (user.getRole().canEdit()) {
            addNav(sidebar, "benchmark", "算法对比", Icons.Type.CHART);
        }
        addNav(sidebar, "about", "设计说明", Icons.Type.INFO);

        sidebar.add(javax.swing.Box.createVerticalGlue());
        sidebar.add(buildSidebarInfo());
        return sidebar;
    }

    private void addNav(JPanel sidebar, String key, String text, Icons.Type iconType) {
        Ui.NavButton button = new Ui.NavButton(text,
                Icons.of(iconType, Theme.SIDEBAR_TEXT),
                Icons.of(iconType, Theme.PRIMARY_DARK));
        button.addActionListener(e -> showCard(key));
        sidebar.add(button);
        sidebar.add(Ui.gap(0, 4));
        navButtons.put(key, button);
    }

    private JPanel buildSidebarInfo() {
        JPanel content = Ui.transparent();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(12, 4, 0, 4)));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = Ui.label("数据概况", 10f, Theme.SIDEBAR_MUTED, Font.BOLD);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Ui.gap(0, 8));
        sidebarStats.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarStats.setFont(Theme.mono(10.3f));
        content.add(sidebarStats);
        content.add(Ui.gap(0, 5));
        sidebarCost.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarCost.setFont(Theme.mono(10.3f));
        content.add(sidebarCost);
        return content;
    }

    private JPanel buildContent() {
        cardHost.setOpaque(false);
        cardHost.setBorder(new EmptyBorder(18, 20, 16, 20));

        overviewPanel = new OverviewPanel(service);
        cardHost.add(overviewPanel, "overview");
        if (user.getRole().canEdit()) {
            cityPanel = new CityPanel(service, this::setStatus);
            cardHost.add(cityPanel, "cities");
        }
        adjacencyPanel = new AdjacencyPanel(service);
        cardHost.add(adjacencyPanel, "adjacency");
        mstPanel = new MstPanel(service);
        cardHost.add(mstPanel, "mst");
        pathPanel = new PathPanel(service, this::setStatus);
        cardHost.add(pathPanel, "path");
        if (user.getRole().canEdit()) {
            benchmarkPanel = new BenchmarkPanel(service);
            cardHost.add(benchmarkPanel, "benchmark");
        }
        aboutPanel = new AboutPanel();
        cardHost.add(aboutPanel, "about");
        return cardHost;
    }

    private JPanel buildStatus() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(Color.WHITE);
        status.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(7, 20, 7, 20)));
        statusLeft.setFont(Theme.font(Font.PLAIN, 10.3f));
        statusRight.setFont(Theme.font(Font.PLAIN, 10.3f));
        status.add(statusLeft, BorderLayout.WEST);
        status.add(statusRight, BorderLayout.EAST);
        return status;
    }

    public void showCard(String key) {
        if (!navButtons.containsKey(key)) {
            return;
        }
        cards.show(cardHost, key);
        for (Map.Entry<String, Ui.NavButton> entry : navButtons.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(key));
        }
    }

    public void setStatus(String text) {
        statusLeft.setText(text);
    }

    public void refreshAll() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        try {
            overviewPanel.refresh();
            if (cityPanel != null) {
                cityPanel.refresh();
            }
            adjacencyPanel.refresh();
            mstPanel.refresh();
            pathPanel.refresh();
            if (benchmarkPanel != null) {
                benchmarkPanel.refresh();
            }
            sidebarStats.setText("V=" + service.cityCount()
                    + " · E=" + service.getGraph().edgeCount());
            sidebarCost.setText(Ui.weight(service.getPrimResult().getTotalWeight())
                    + " · " + service.getPrimResult().getEdges().size() + " 边");
            statusRight.setText("最近重建 " + Ui.millis(service.getRebuildMillis())
                    + " · Prim = Kruskal = "
                    + Ui.weight(service.getPrimResult().getTotalWeight()));
            String warning = service.consumeWarning();
            if (warning != null) {
                statusLeft.setText(warning);
            }
        } finally {
            refreshing = false;
        }
    }

    private void logout() {
        boolean ok = Ui.confirm(this, "退出登录", "确定退出当前账号并返回登录页吗？");
        if (!ok) {
            return;
        }
        dispose();
        new LoginFrame(service).setVisible(true);
    }

    public void demoPath(String from, String to) {
        pathPanel.demoQuery(from, to, false);
        showCard("path");
    }

    public void demoSort(String pivot) {
        if (cityPanel != null) {
            cityPanel.demoSort(pivot);
            showCard("cities");
        }
    }

    public void demoBenchmark() {
        if (benchmarkPanel != null) {
            benchmarkPanel.demoBenchmarkSync();
            showCard("benchmark");
        }
    }
}
