package logistics.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * 侧边栏用的矢量小图标，避免引入图片资源。
 */
public final class Icons {

    public enum Type {
        MAP, CITY, LIST, TREE, PATH, CHART, INFO, LOGOUT
    }

    private Icons() {
    }

    public static Icon of(Type type, Color color) {
        return new VectorIcon(type, color);
    }

    private static final class VectorIcon implements Icon {
        private final Type type;
        private final Color color;

        VectorIcon(Type type, Color color) {
            this.type = type;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case MAP -> {
                    g2.drawRoundRect(2, 3, 14, 12, 3, 3);
                    g2.drawLine(5, 12, 8, 8);
                    g2.drawLine(8, 8, 12, 10);
                    g2.drawLine(12, 10, 15, 6);
                    g2.fillOval(4, 11, 2, 2);
                    g2.fillOval(11, 9, 2, 2);
                    g2.fillOval(14, 5, 2, 2);
                }
                case CITY -> {
                    g2.drawLine(4, 13, 9, 5);
                    g2.drawLine(9, 5, 14, 12);
                    g2.drawLine(4, 13, 14, 12);
                    g2.fillOval(2, 11, 4, 4);
                    g2.fillOval(7, 3, 4, 4);
                    g2.fillOval(12, 10, 4, 4);
                }
                case LIST -> {
                    for (int row = 0; row < 3; row++) {
                        int yy = 5 + row * 4;
                        g2.fillOval(2, yy - 1, 3, 3);
                        g2.drawLine(7, yy + 1, 15, yy + 1);
                    }
                }
                case TREE -> {
                    g2.drawLine(9, 5, 4, 13);
                    g2.drawLine(9, 5, 9, 12);
                    g2.drawLine(9, 5, 14, 13);
                    g2.fillOval(6, 2, 6, 6);
                    g2.fillOval(2, 11, 4, 4);
                    g2.fillOval(7, 10, 4, 4);
                    g2.fillOval(12, 11, 4, 4);
                }
                case PATH -> {
                    g2.drawLine(4, 14, 8, 8);
                    g2.drawLine(8, 8, 12, 10);
                    g2.drawLine(12, 10, 14, 4);
                    g2.fillOval(2, 12, 4, 4);
                    g2.fillOval(12, 2, 4, 4);
                }
                case CHART -> {
                    g2.drawLine(3, 15, 15, 15);
                    g2.drawRoundRect(4, 9, 3, 6, 1, 1);
                    g2.drawRoundRect(8, 5, 3, 10, 1, 1);
                    g2.drawRoundRect(12, 7, 3, 8, 1, 1);
                }
                case INFO -> {
                    g2.drawOval(2, 2, 14, 14);
                    g2.fillOval(8, 5, 2, 2);
                    g2.drawLine(9, 9, 9, 13);
                }
                case LOGOUT -> {
                    g2.drawLine(9, 3, 3, 3);
                    g2.drawLine(3, 3, 3, 15);
                    g2.drawLine(3, 15, 9, 15);
                    g2.drawLine(7, 9, 15, 9);
                    g2.drawLine(12, 6, 15, 9);
                    g2.drawLine(12, 12, 15, 9);
                }
            }
            g2.dispose();
        }
    }
}
