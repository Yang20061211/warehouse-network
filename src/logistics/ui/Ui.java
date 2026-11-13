package logistics.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * 自绘组件与界面小工具。
 */
public final class Ui {

    private Ui() {
    }

    public static JPanel transparent() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static Component gap(int width, int height) {
        return javax.swing.Box.createRigidArea(new Dimension(width, height));
    }

    public static JLabel label(String text, float size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(style, Math.max(11f, size)));
        label.setForeground(color);
        return label;
    }

    public static JPanel cardTitle(String title) {
        JPanel panel = transparent(new BorderLayout(0, 0));
        panel.add(label(title, 15.5f, Theme.TEXT, Font.BOLD), BorderLayout.WEST);
        return panel;
    }

    public static String weight(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String millis(double ms) {
        if (ms >= 1) {
            return String.format(Locale.ROOT, "%.3f ms", ms);
        }
        if (ms >= 0.001) {
            return String.format(Locale.ROOT, "%.4f ms", ms);
        }
        return String.format(Locale.ROOT, "%.1f µs", ms * 1000);
    }

    /** 圆角面板，负责绘制背景和 1px 边框。 */
    public static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color stroke;
        private final int radius;

        public RoundedPanel(LayoutManager layout, Color fill, Color stroke, int radius) {
            super(layout);
            this.fill = fill;
            this.stroke = stroke;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (stroke != null) {
                g2.setColor(stroke);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static RoundedPanel card() {
        return new RoundedPanel(new BorderLayout(), Theme.CARD, Theme.BORDER, 8);
    }

    public enum ButtonStyle {
        PRIMARY, SECONDARY, GHOST, DANGER, DARK
    }

    /** 圆角按钮，带 hover / pressed / disabled 状态。 */
    public static final class PillButton extends javax.swing.JButton {
        private final ButtonStyle style;
        private boolean hover;

        public PillButton(String text, ButtonStyle style) {
            super(text);
            this.style = style;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.font(Font.PLAIN, 12.5f));
            setBorder(new EmptyBorder(8, 15, 8, 15));
            setForeground(foregroundColor());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
            addPropertyChangeListener("enabled", evt -> {
                setForeground(foregroundColor());
                repaint();
            });
        }

        private Color foregroundColor() {
            if (!isEnabled()) {
                return Theme.FAINT;
            }
            return switch (style) {
                case PRIMARY, DARK -> Color.WHITE;
                case DANGER -> Theme.DANGER;
                case GHOST -> Theme.MUTED;
                case SECONDARY -> Theme.TEXT;
            };
        }

        private Color backgroundColor() {
            if (!isEnabled()) {
                return new Color(0xF0, 0xF2, 0xEF);
            }
            boolean down = getModel().isPressed();
            return switch (style) {
                case PRIMARY, DARK -> down ? Theme.TEXT.darker() : hover ? Theme.TEXT_SOFT : Theme.TEXT;
                case DANGER -> down ? new Color(0xFE, 0xE2, 0xE2) : hover ? new Color(0xFE, 0xE8, 0xE8) : Theme.DANGER_SOFT;
                case GHOST -> hover ? new Color(0xEE, 0xF1, 0xED) : new Color(0, 0, 0, 0);
                case SECONDARY -> hover ? new Color(0xF7, 0xF8, 0xF6) : Color.WHITE;
            };
        }

        private Color borderColor() {
            if (!isEnabled()) {
                return Theme.BORDER;
            }
            return switch (style) {
                case SECONDARY -> Theme.BORDER_STRONG;
                case DANGER -> new Color(0xFE, 0xCA, 0xCA);
                case GHOST -> new Color(0, 0, 0, 0);
                default -> new Color(0, 0, 0, 0);
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            Color border = borderColor();
            if (border.getAlpha() > 0) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            }
            if (isFocusOwner()) {
                g2.setColor(style == ButtonStyle.PRIMARY || style == ButtonStyle.DARK
                        ? Color.WHITE : Theme.PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 4, 4);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 侧边栏导航按钮：图标 + 文案 + 激活高亮。 */
    public static final class NavButton extends javax.swing.JButton {
        private final Icon idleIcon;
        private final Icon activeIcon;
        private boolean active;
        private boolean hover;

        public NavButton(String text, Icon idleIcon, Icon activeIcon) {
            super(text, idleIcon);
            this.idleIcon = idleIcon;
            this.activeIcon = activeIcon;
            setFont(Theme.font(Font.PLAIN, 13f));
            setForeground(Theme.SIDEBAR_TEXT);
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(12);
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        public void setActive(boolean active) {
            this.active = active;
            setIcon(active ? activeIcon : idleIcon);
            setForeground(active ? Theme.TEXT : Theme.SIDEBAR_TEXT);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(Theme.SIDEBAR_ACTIVE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
            } else if (hover) {
                g2.setColor(Theme.SIDEBAR_ITEM);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 7, 7);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 圆角小标签。 */
    public static final class BadgeLabel extends JLabel {
        private final Color background;

        public BadgeLabel(String text, Color foreground, Color background) {
            super(text);
            this.background = background;
            setForeground(foreground);
            setFont(Theme.font(Font.BOLD, 11f));
            setBorder(new EmptyBorder(4, 9, 4, 9));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 指标卡：标题 / 大数字 / 注释。 */
    public static final class StatTile extends RoundedPanel {
        private final JLabel valueLabel;
        private final JLabel hintLabel;

        public StatTile(String caption, String value, String hint, Color accent) {
            super(null, Theme.CARD, Theme.BORDER, 6);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(11, 14, 11, 14));

            JLabel captionLabel = label(caption, 10.5f, Theme.MUTED, Font.PLAIN);
            captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(captionLabel);
            add(gap(0, 4));

            valueLabel = label(value, 21f, accent, Font.BOLD);
            valueLabel.setFont(Theme.display(Font.BOLD, 21f));
            valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(valueLabel);
            if (hint == null || hint.isBlank()) {
                hintLabel = null;
            } else {
                add(gap(0, 3));
                hintLabel = label(hint, 10.2f, Theme.FAINT, Font.PLAIN);
                hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(hintLabel);
            }
        }

        public void setValue(String value) {
            valueLabel.setText(value);
        }

        public void setHint(String hint) {
            if (hintLabel != null) {
                hintLabel.setText(hint == null ? "" : hint);
                hintLabel.setVisible(hint != null && !hint.isBlank());
            }
        }

        public void setValueColor(Color color) {
            valueLabel.setForeground(color);
        }
    }

    /** 圆角边框，负责文字内边距。 */
    public static Border roundedBorder(Color color, int radius, int padH, int padV) {
        return new AbstractBorder() {
            @Override
            public java.awt.Insets getBorderInsets(Component c) {
                return new java.awt.Insets(padV, padH, padV, padH);
            }

            @Override
            public java.awt.Insets getBorderInsets(Component c, java.awt.Insets insets) {
                insets.set(padV, padH, padV, padH);
                return insets;
            }

            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
                g2.dispose();
            }
        };
    }

    public static void styleField(JTextField field) {
        field.setFont(Theme.font(Font.PLAIN, 13f));
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.PRIMARY);
        field.setBackground(Color.WHITE);
        field.setBorder(roundedBorder(Theme.BORDER_STRONG, 6, 12, 9));
        field.setPreferredSize(new Dimension(220, 38));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(roundedBorder(Theme.PRIMARY, 6, 12, 9));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(roundedBorder(Theme.BORDER_STRONG, 6, 12, 9));
            }
        });
    }

    public static void stylePassword(JPasswordField field) {
        styleField(field);
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(Theme.font(Font.PLAIN, 12.5f));
        combo.setBackground(Color.WHITE);
        combo.setForeground(Theme.TEXT);
        combo.setBorder(BorderFactory.createCompoundBorder(
                roundedBorder(Theme.BORDER_STRONG, 6, 8, 6),
                new EmptyBorder(1, 2, 1, 2)));
        combo.setPreferredSize(new Dimension(150, 36));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(Color.WHITE);
        pane.setBackground(Color.WHITE);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        pane.getHorizontalScrollBar().setUnitIncrement(18);
        return pane;
    }

    public static void styleTable(JTable table) {
        table.setFont(Theme.font(Font.PLAIN, 13f));
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(Theme.SIDEBAR_ACTIVE);
        table.setSelectionForeground(Theme.TEXT);
        table.setAutoCreateRowSorter(false);
        table.getTableHeader().setFont(Theme.font(Font.PLAIN, 11.8f));
        table.getTableHeader().setForeground(Theme.MUTED);
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.setDefaultRenderer(Object.class, new CellRenderer(false));
        table.setDefaultRenderer(Number.class, new CellRenderer(true));
    }

    public static void alignNumericColumns(JTable table, int... columns) {
        for (int index : columns) {
            TableColumn column = table.getColumnModel().getColumn(index);
            column.setCellRenderer(new CellRenderer(true));
            column.setHeaderRenderer(new HeaderRenderer(true));
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private final boolean rightAligned;

        HeaderRenderer() {
            this(false);
        }

        HeaderRenderer(boolean rightAligned) {
            this.rightAligned = rightAligned;
            setOpaque(true);
            setBackground(Color.WHITE);
            setForeground(Theme.MUTED);
            setFont(Theme.font(Font.PLAIN, 11.8f));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(rightAligned ? SwingConstants.RIGHT : SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setBackground(Color.WHITE);
            setForeground(Theme.MUTED);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(rightAligned ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private final boolean rightAligned;

        CellRenderer(boolean rightAligned) {
            this.rightAligned = rightAligned;
            setOpaque(true);
            setHorizontalAlignment(rightAligned ? SwingConstants.RIGHT : SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setFont(table.getFont());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.RULE),
                    new EmptyBorder(0, 10, 0, 10)));
            setHorizontalAlignment(rightAligned ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (selected) {
                setBackground(Theme.SIDEBAR_ACTIVE);
                setForeground(Theme.TEXT);
            } else {
                setBackground(Color.WHITE);
                setForeground(Theme.TEXT_SOFT);
            }
            return this;
        }
    }

    public static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "操作失败", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    /** 绘制水平迷你图例线。 */
    public static JComponent legendLine(Color color, int width) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(width, 12));
                setMinimumSize(new Dimension(width, 12));
                setMaximumSize(new Dimension(width, 12));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(7, 6, getWidth() - 7, 6);
                g2.dispose();
            }
        };
    }

    /** 圆形小圆点图例。 */
    public static JComponent legendDot(Color fill, Color stroke) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(14, 14));
                setMinimumSize(new Dimension(14, 14));
                setMaximumSize(new Dimension(14, 14));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Double(2, 2, 10, 10, 10, 10));
                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(new RoundRectangle2D.Double(2, 2, 10, 10, 10, 10));
                g2.dispose();
            }
        };
    }
}
