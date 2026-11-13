package logistics.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

/**
 * 全局视觉规范：颜色、字体、Swing 默认值。
 */
public final class Theme {

    public static final Color BG = new Color(0xF4, 0xF7, 0xFB);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE3, 0xE9, 0xF2);
    public static final Color BORDER_STRONG = new Color(0xD3, 0xDC, 0xE9);
    public static final Color TEXT = new Color(0x0F, 0x17, 0x2A);
    public static final Color TEXT_SOFT = new Color(0x33, 0x41, 0x55);
    public static final Color MUTED = new Color(0x64, 0x74, 0x8B);
    public static final Color FAINT = new Color(0x94, 0xA3, 0xB8);
    public static final Color PRIMARY = new Color(0x25, 0x63, 0xEB);
    public static final Color PRIMARY_DARK = new Color(0x1D, 0x4E, 0xD8);
    public static final Color PRIMARY_SOFT = new Color(0xEF, 0xF5, 0xFF);
    public static final Color SKY = new Color(0x0E, 0xA5, 0xE9);
    public static final Color SUCCESS = new Color(0x05, 0x96, 0x69);
    public static final Color SUCCESS_SOFT = new Color(0xEC, 0xFD, 0xF5);
    public static final Color WARNING = new Color(0xD9, 0x77, 0x06);
    public static final Color WARNING_SOFT = new Color(0xFF, 0xFB, 0xEB);
    public static final Color DANGER = new Color(0xDC, 0x26, 0x26);
    public static final Color DANGER_SOFT = new Color(0xFE, 0xF2, 0xF2);
    public static final Color SIDEBAR = new Color(0x0B, 0x15, 0x26);
    public static final Color SIDEBAR_ITEM = new Color(0x15, 0x22, 0x38);
    public static final Color SIDEBAR_TEXT = new Color(0xA7, 0xB4, 0xC9);
    public static final Color SIDEBAR_MUTED = new Color(0x64, 0x74, 0x8B);
    public static final Color SIDEBAR_ACTIVE = new Color(0x1E, 0x3A, 0x8A);

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
