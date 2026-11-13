package logistics.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * 全局视觉规范：颜色、字体、Swing 默认值。
 */
public final class Theme {

    public static final Color BG = new Color(0xF3, 0xF4, 0xF1);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xD9, 0xDE, 0xD8);
    public static final Color BORDER_STRONG = new Color(0xB9, 0xC2, 0xBA);
    public static final Color TEXT = new Color(0x20, 0x26, 0x22);
    public static final Color TEXT_SOFT = new Color(0x3B, 0x44, 0x3E);
    public static final Color MUTED = new Color(0x66, 0x73, 0x6A);
    public static final Color FAINT = new Color(0x65, 0x70, 0x67);
    public static final Color PRIMARY = new Color(0x37, 0x65, 0x53);
    public static final Color PRIMARY_DARK = new Color(0x23, 0x4B, 0x3D);
    public static final Color PRIMARY_SOFT = new Color(0xE8, 0xF0, 0xEB);
    public static final Color SUCCESS = new Color(0x2E, 0x7D, 0x5A);
    public static final Color SUCCESS_SOFT = new Color(0xE9, 0xF4, 0xEE);
    public static final Color WARNING = new Color(0xA6, 0x63, 0x00);
    public static final Color WARNING_SOFT = new Color(0xFF, 0xF4, 0xE1);
    public static final Color DANGER = new Color(0xB4, 0x23, 0x18);
    public static final Color DANGER_SOFT = new Color(0xFD, 0xEC, 0xEA);
    public static final Color RULE = new Color(0xEE, 0xF0, 0xED);
    public static final Color SIDEBAR = new Color(0xED, 0xEF, 0xEC);
    public static final Color SIDEBAR_ITEM = new Color(0xE4, 0xE7, 0xE3);
    public static final Color SIDEBAR_TEXT = new Color(0x4B, 0x5B, 0x51);
    public static final Color SIDEBAR_MUTED = new Color(0x5F, 0x6B, 0x62);
    public static final Color SIDEBAR_ACTIVE = new Color(0xE0, 0xE4, 0xDF);

    private static final String FAMILY = pickFamily("PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "SansSerif");
    private static final String MONO = pickFamily("SF Mono", "Menlo", "Monaco", "Courier New");
    private static final String DISPLAY = pickFamily("PingFang SC", "Helvetica Neue", "SansSerif");

    private Theme() {
    }

    private static String pickFamily(String... candidates) {
        String[] available = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String candidate : candidates) {
            for (String name : available) {
                if (name.equalsIgnoreCase(candidate)) {
                    return name;
                }
            }
        }
        return candidates[candidates.length - 1];
    }

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, 12).deriveFont(size);
    }

    public static Font display(int style, float size) {
        return new Font(DISPLAY, style, 12).deriveFont(size);
    }

    public static Font mono(float size) {
        return new Font(MONO, Font.PLAIN, 12).deriveFont(size);
    }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** 让 Swing 默认组件也使用统一字体。 */
    public static void install() {
        String[] keys = {
                "Label.font", "Button.font", "ToggleButton.font", "CheckBox.font",
                "RadioButton.font", "TextField.font", "PasswordField.font",
                "TextArea.font", "TextPane.font", "EditorPane.font", "ComboBox.font",
                "List.font", "Table.font", "TableHeader.font", "Menu.font",
                "MenuItem.font", "OptionPane.messageFont", "ToolTip.font",
                "Spinner.font", "FormattedTextField.font"
        };
        Font base = font(Font.PLAIN, 13f);
        for (String key : keys) {
            UIManager.put(key, base);
        }
        UIManager.put("Table.selectionBackground", PRIMARY_SOFT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Panel.background", BG);
        UIManager.put("ToolTip.background", new Color(0x0F, 0x17, 0x2A));
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("ComboBox.background", Color.WHITE);
    }
}
