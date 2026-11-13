package logistics.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import logistics.app.Auth;
import logistics.app.NetworkService;
import logistics.app.Role;
import logistics.app.User;

/** 登录窗口：账号登录，并提供管理员和查询员演示入口。 */
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
        setSize(460, 560);
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG);

        Ui.RoundedPanel card = new Ui.RoundedPanel(new BorderLayout(), Theme.CARD, Theme.BORDER, 8);
        card.setPreferredSize(new Dimension(390, 478));
        card.setBorder(new EmptyBorder(30, 32, 28, 32));
        card.add(buildForm(), BorderLayout.CENTER);
        root.add(card);
        return root;
    }

    private JPanel buildForm() {
        JPanel form = Ui.transparent();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel heading = Ui.label("城际仓网规划系统", 24f, Theme.TEXT, Font.BOLD);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Ui.gap(0, 38));
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
        JLabel hint = Ui.label("演示登录", 10f, Theme.MUTED, Font.PLAIN);
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
        JLabel credentials = Ui.label("admin / admin123     viewer / viewer123", 10f,
                Theme.MUTED, Font.PLAIN);
        credentials.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(credentials);

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

}
