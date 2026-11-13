package logistics.ui;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import logistics.app.Auth;
import logistics.app.NetworkService;
import logistics.app.Role;
import logistics.app.User;

/**
 * 离屏截图工具：把登录页和主要功能页渲染成 PNG。
 * 用 printAll 直接画进 BufferedImage，不依赖屏幕录制权限。
 */
public final class ShotRunner {

    private ShotRunner() {
    }

    public static void run(NetworkService service, Path dir) throws Exception {
        Files.createDirectories(dir);

        // 登录页
        final LoginFrame[] login = new LoginFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            login[0] = new LoginFrame(service);
            login[0].setVisible(true);
        });
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(() -> login[0].clearError());
        Thread.sleep(120);
        capture(login[0].getContentPane(), dir.resolve("login.png"));
        SwingUtilities.invokeAndWait(() -> login[0].dispose());

        User admin = Auth.demoUser(Role.ADMIN);
        final MainFrame[] main = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            main[0] = new MainFrame(service, admin);
            main[0].setVisible(true);
        });
        Thread.sleep(900);

        capture(main[0].getContentPane(), dir.resolve("overview.png"));

        SwingUtilities.invokeAndWait(() -> main[0].showCard("cities"));
        Thread.sleep(350);
        capture(main[0].getContentPane(), dir.resolve("cities.png"));

        SwingUtilities.invokeAndWait(() -> main[0].demoSort("北京"));
        Thread.sleep(450);
        capture(main[0].getContentPane(), dir.resolve("sorted.png"));

        SwingUtilities.invokeAndWait(() -> main[0].showCard("adjacency"));
        Thread.sleep(350);
        capture(main[0].getContentPane(), dir.resolve("adjacency.png"));

        SwingUtilities.invokeAndWait(() -> main[0].showCard("mst"));
        Thread.sleep(350);
        capture(main[0].getContentPane(), dir.resolve("mst.png"));

        SwingUtilities.invokeAndWait(() -> main[0].demoPath("北京", "乌鲁木齐"));
        Thread.sleep(500);
        capture(main[0].getContentPane(), dir.resolve("path.png"));

        SwingUtilities.invokeAndWait(() -> main[0].demoPath("拉萨", "上海"));
        Thread.sleep(500);
        capture(main[0].getContentPane(), dir.resolve("path-2.png"));

        SwingUtilities.invokeAndWait(() -> main[0].demoBenchmark());
        Thread.sleep(600);
        capture(main[0].getContentPane(), dir.resolve("benchmark.png"));

        SwingUtilities.invokeAndWait(() -> main[0].showCard("about"));
        Thread.sleep(350);
        capture(main[0].getContentPane(), dir.resolve("about.png"));

        SwingUtilities.invokeAndWait(() -> main[0].dispose());

        User viewer = Auth.demoUser(Role.VIEWER);
        final MainFrame[] viewerFrame = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            viewerFrame[0] = new MainFrame(service, viewer);
            viewerFrame[0].setVisible(true);
        });
        Thread.sleep(700);
        capture(viewerFrame[0].getContentPane(), dir.resolve("viewer-overview.png"));
        SwingUtilities.invokeAndWait(() -> viewerFrame[0].demoPath("广州", "哈尔滨"));
        Thread.sleep(400);
        capture(viewerFrame[0].getContentPane(), dir.resolve("viewer-path.png"));
        SwingUtilities.invokeAndWait(() -> viewerFrame[0].dispose());

        // 截图过程实际执行过排序，恢复默认数据，保持交付数据整洁
        service.resetDefaults();
        System.out.println("截图完成：" + dir.toAbsolutePath());
        System.exit(0);
    }

    private static void capture(Component component, Path target) throws IOException {
        int width = Math.max(1, component.getWidth());
        int height = Math.max(1, component.getHeight());
        int scale = 2;
        BufferedImage image = new BufferedImage(width * scale, height * scale,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.scale(scale, scale);
        component.printAll(g2);
        g2.dispose();
        File file = target.toFile();
        ImageIO.write(image, "png", file);
        System.out.println("saved " + file + " (" + image.getWidth() + "x" + image.getHeight() + ")");
    }
}
