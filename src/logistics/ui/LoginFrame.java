package logistics.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import logistics.app.Auth;
import logistics.app.NetworkService;
import logistics.app.Role;
import logistics.app.User;

/**
 * 登录页：左侧品牌视觉，右侧账号表单。
 * 提供管理员 / 查询员两个演示入口，体现角色隔离。
 */
public final class LoginFrame extends JFrame {

    private final NetworkService service;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel errorLabel = new JLabel(" ");
    private Ui.PillButton loginButton;

    public LoginFrame(NetworkService service) {
        super("城际仓网规划系统 · 登录");
        this.service = service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(buildContent());
        setSize(1020, 660);
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(new BrandPanel(), BorderLayout.CENTER);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        right.setPreferredSize(new Dimension(470, 0));
        right.add(buildForm());
        root.add(right, BorderLayout.EAST);
        return root;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(360, 585));

        JPanel logoRow = new JPanel();
        logoRow.setOpaque(false);
        logoRow.setLayout(new BoxLayout(logoRow, BoxLayout.X_AXIS));
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        Ui.LogoMark logo = new Ui.LogoMark(40);
        logo.setAlignmentY(Component.CENTER_ALIGNMENT);
        logoRow.add(logo);
        logoRow.add(Ui.gap(12, 0));
        JPanel logoText = new JPanel();
        logoText.setOpaque(false);
        logoText.setLayout(new BoxLayout(logoText, BoxLayout.Y_AXIS));
        JLabel brand = Ui.label("城际仓网", 14.5f, Theme.TEXT, Font.BOLD);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = Ui.label("CITY NETWORK STUDIO", 8.5f, Theme.FAINT, Font.BOLD);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoText.add(brand);
        logoText.add(Ui.gap(0, 1));
        logoText.add(sub);
        logoRow.add(logoText);
        form.add(logoRow);

        form.add(Ui.gap(0, 34));
        JLabel heading = Ui.label("欢迎登录", 28f, Theme.TEXT, Font.BOLD);
        heading.setFont(Theme.display(Font.BOLD, 28f));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Ui.gap(0, 7));
        JLabel subtitle = Ui.label("选择身份，进入 30 城光纤仓网规划系统", 11.5f,
                Theme.MUTED, Font.PLAIN);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(subtitle);

        form.add(Ui.gap(0, 28));
        JLabel accountLabel = Ui.label("账号", 11f, Theme.TEXT_SOFT, Font.BOLD);
        accountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(accountLabel);
        form.add(Ui.gap(0, 7));
        Ui.styleField(usernameField);
        usernameField.setToolTipText("演示账号：admin / viewer");
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(usernameField);

        form.add(Ui.gap(0, 16));
        JLabel passwordLabel = Ui.label("密码", 11f, Theme.TEXT_SOFT, Font.BOLD);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passwordLabel);
        form.add(Ui.gap(0, 7));
        Ui.stylePassword(passwordField);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.addActionListener(e -> doLogin());
        usernameField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                clearError();
            }
        });
        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                clearError();
            }
        });
        form.add(passwordField);

        form.add(Ui.gap(0, 9));
        errorLabel.setFont(Theme.font(Font.PLAIN, 11f));
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(errorLabel);

        form.add(Ui.gap(0, 8));
        loginButton = new Ui.PillButton("登 录", Ui.ButtonStyle.PRIMARY);
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setFont(Theme.font(Font.BOLD, 13.5f));
        loginButton.addActionListener(e -> doLogin());
        form.add(loginButton);

        form.add(Ui.gap(0, 22));
        JPanel divider = new JPanel(new BorderLayout());
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = Ui.label("或使用演示账号", 9.8f, Theme.FAINT, Font.PLAIN);
        hint.setHorizontalAlignment(JLabel.CENTER);
        divider.add(hint, BorderLayout.CENTER);
        form.add(divider);

        form.add(Ui.gap(0, 9));
        JPanel demoRow = new JPanel(new GridLayout(1, 2, 10, 0));
        demoRow.setOpaque(false);
        demoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        demoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        Ui.PillButton adminDemo = new Ui.PillButton("管理员演示", Ui.ButtonStyle.SECONDARY);
        Ui.PillButton viewerDemo = new Ui.PillButton("查询员演示", Ui.ButtonStyle.SECONDARY);
        adminDemo.setFont(Theme.font(Font.PLAIN, 12f));
        viewerDemo.setFont(Theme.font(Font.PLAIN, 12f));
        adminDemo.addActionListener(e -> demoLogin(Role.ADMIN));
        viewerDemo.addActionListener(e -> demoLogin(Role.VIEWER));
        demoRow.add(adminDemo);
        demoRow.add(viewerDemo);
        form.add(demoRow);

        form.add(Ui.gap(0, 9));
        JLabel credentials = Ui.label("admin / admin123      viewer / viewer123", 9.3f,
                Theme.FAINT, Font.PLAIN);
        credentials.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(credentials);

        form.add(Ui.gap(0, 26));
        JLabel footer = Ui.label("数据结构课程设计 · 线性表 + 图 + 最小生成树", 9.5f,
                Theme.FAINT, Font.PLAIN);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(footer);

        return form;
    }

    /** 截图脚本调用：确保登录页展示为初始状态。 */
    public void clearError() {
        errorLabel.setText(" ");
    }

    private void demoLogin(Role role) {
        User demo = Auth.demoUser(role);
        usernameField.setText(demo.getUsername());
        passwordField.setText(role == Role.ADMIN ? "admin123" : "viewer123");
        doLogin();
    }

    private void doLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        if (username.isBlank() || password.isEmpty()) {
            errorLabel.setText("请输入账号和密码");
            return;
        }
        User user = Auth.login(username, password);
        if (user == null) {
            errorLabel.setText("账号或密码不正确，请重试");
            passwordField.selectAll();
            passwordField.requestFocusInWindow();
            return;
        }
        errorLabel.setText(" ");
        MainFrame frame = new MainFrame(service, user);
        frame.setVisible(true);
        dispose();
    }

    /** 左侧深色品牌区：渐变、网络示意和标题。 */
    private static final class BrandPanel extends JPanel {
        private static final double[][] NODES = {
                {0.52, 0.16}, {0.63, 0.10}, {0.74, 0.19}, {0.84, 0.12},
                {0.90, 0.28}, {0.78, 0.33}, {0.66, 0.29}, {0.58, 0.41},
                {0.70, 0.48}, {0.82, 0.45}, {0.91, 0.54}, {0.75, 0.60},
                {0.62, 0.56}, {0.53, 0.68}, {0.68, 0.73}, {0.85, 0.68}
        };
        private static final int[][] EDGES = {
                {0, 1}, {1, 2}, {2, 3}, {3, 4}, {2, 5}, {5, 6}, {6, 0},
                {1, 6}, {5, 8}, {6, 7}, {7, 8}, {8, 9}, {4, 9}, {9, 10},
                {8, 11}, {11, 12}, {7, 12}, {11, 14}, {12, 13}, {13, 14},
                {10, 15}, {11, 15}, {3, 4}, {0, 7}, {8, 5}
        };

        BrandPanel() {
            setOpaque(true);
            setPreferredSize(new Dimension(560, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setPaint(new GradientPaint(0, 0, new Color(0x0A, 0x12, 0x22),
                    w, h, new Color(0x10, 0x2B, 0x5E)));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Float(w * 0.82f, h * 0.20f),
                    280f, new float[]{0f, 1f},
                    new Color[]{new Color(37, 99, 235, 90), new Color(37, 99, 235, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Float(w * 0.08f, h * 0.92f),
                    260f, new float[]{0f, 1f},
                    new Color[]{new Color(14, 165, 233, 55), new Color(14, 165, 233, 0)}));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(255, 255, 255, 10));
            for (int x = 0; x < w; x += 56) {
                g2.drawLine(x, 0, x, h);
            }
            for (int y = 0; y < h; y += 56) {
                g2.drawLine(0, y, w, y);
            }

            drawNetwork(g2, w, h);
            drawText(g2, w, h);
            g2.dispose();
        }

        private void drawNetwork(Graphics2D g2, int w, int h) {
            int[] xs = new int[NODES.length];
            int[] ys = new int[NODES.length];
            for (int i = 0; i < NODES.length; i++) {
                xs[i] = (int) (NODES[i][0] * w);
                ys[i] = (int) (NODES[i][1] * h * 0.82);
            }
            g2.setStroke(new BasicStroke(1.1f));
            for (int[] edge : EDGES) {
                g2.setColor(new Color(147, 197, 253, 55));
                g2.drawLine(xs[edge[0]], ys[edge[0]], xs[edge[1]], ys[edge[1]]);
            }
            // 高亮一条“MST”路径
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(96, 165, 250, 120));
            int[] path = {13, 12, 7, 8, 11, 15};
            for (int i = 0; i + 1 < path.length; i++) {
                g2.drawLine(xs[path[i]], ys[path[i]], xs[path[i + 1]], ys[path[i + 1]]);
            }
            for (int i = 0; i < NODES.length; i++) {
                boolean hot = i == 7 || i == 8 || i == 11 || i == 15 || i == 13;
                int r = hot ? 5 : 3;
                g2.setColor(new Color(59, 130, 246, 55));
                g2.fillOval(xs[i] - r - 3, ys[i] - r - 3, (r + 3) * 2, (r + 3) * 2);
                g2.setColor(hot ? new Color(0xBF, 0xDB, 0xFE) : new Color(0x93, 0xC5, 0xFD, 190));
                g2.fillOval(xs[i] - r, ys[i] - r, r * 2, r * 2);
                g2.setColor(new Color(0x0A, 0x12, 0x22));
                g2.fillOval(xs[i] - 1, ys[i] - 1, 2, 2);
            }
        }

        private void drawText(Graphics2D g2, int w, int h) {
            int left = 48;
            int y = h - 232;
            g2.setFont(Theme.font(Font.BOLD, 9.5f));
            g2.setColor(new Color(0x93, 0xC5, 0xFD));
            g2.drawString("DATA STRUCTURES  ·  COURSE PROJECT", left, y);

            g2.setFont(Theme.display(Font.BOLD, 31f));
            g2.setColor(Color.WHITE);
            g2.drawString("城际仓网规划系统", left, y + 48);

            g2.setFont(Theme.font(Font.PLAIN, 12.5f));
            g2.setColor(new Color(0xBF, 0xDB, 0xFE));
            g2.drawString("线性表 + 图 + 最小生成树  ·  递进式工程实现", left, y + 80);

            g2.setFont(Theme.font(Font.PLAIN, 10.5f));
            g2.setColor(new Color(0xCB, 0xD5, 0xE1));
            g2.drawString("30 个城市节点 · 光纤成本最低 · 毫秒级路径查询", left, y + 106);

            String[] chips = {"双向循环链表", "邻接表", "Prim / Kruskal", "Dijkstra"};
            Font chipFont = Theme.font(Font.PLAIN, 9.8f);
            g2.setFont(chipFont);
            FontMetrics fm = g2.getFontMetrics();
            int x = left;
            int chipY = h - 64;
            for (String chip : chips) {
                int cw = fm.stringWidth(chip) + 22;
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fill(new RoundRectangle2D.Double(x, chipY, cw, 26, 13, 13));
                g2.setColor(new Color(255, 255, 255, 45));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(x, chipY, cw, 26, 13, 13));
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                g2.drawString(chip, x + 11, chipY + 17);
                x += cw + 8;
            }

            g2.setFont(Theme.font(Font.PLAIN, 8.5f));
            g2.setColor(new Color(0x64, 0x74, 0x8B));
            g2.drawString("© 2026  CITY NETWORK STUDIO", left, h - 26);
        }
    }
}
