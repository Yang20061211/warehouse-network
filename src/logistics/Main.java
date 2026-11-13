package logistics;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import logistics.app.NetworkService;
import logistics.app.SelfTest;
import logistics.ui.LoginFrame;
import logistics.ui.ShotRunner;
import logistics.ui.Theme;

/**
 * 程序入口。
 *
 * <ul>
 *   <li>默认：打开图形界面</li>
 *   <li>{@code --selftest}：命令行自检，输出三阶段结果与性能数据</li>
 *   <li>{@code --shots 目录}：离屏渲染各页面截图（用于报告/PPT）</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "城际仓网规划系统");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 使用 Swing 默认外观即可
        }
        Theme.install();

        Path dataFile = Paths.get(System.getProperty("user.dir"), "data", "cities.txt");
        NetworkService service = new NetworkService(dataFile);

        if (args.length > 0 && "--selftest".equals(args[0])) {
            SelfTest.run(service);
            return;
        }
        if (args.length > 0 && "--shots".equals(args[0])) {
            Path dir = args.length > 1 ? Paths.get(args[1]) : Paths.get("shots");
            ShotRunner.run(service, dir);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame(service);
            frame.setVisible(true);
        });
    }
}
