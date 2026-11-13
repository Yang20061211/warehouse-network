package logistics.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import logistics.app.NetworkService;

/**
 * 邻接表全文展示。每条无向边在两个端点的边链表中各出现一次。
 */
public final class AdjacencyPanel extends JPanel {

    private final NetworkService service;
    private final JTextArea area = new JTextArea();
    private final JLabel statsLabel = Ui.label("—", 10.5f, Theme.MUTED, Font.PLAIN);

    public AdjacencyPanel(NetworkService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout());

        Ui.RoundedPanel card = Ui.card();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel header = Ui.transparent(new BorderLayout());
        JPanel titlePanel = Ui.cardTitle("邻接表存储结构", "顶点数组与边结点链表，每条无向边存两次");
        header.add(titlePanel, BorderLayout.WEST);

        JPanel actions = Ui.transparent();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        Ui.PillButton copy = new Ui.PillButton("复制全文", Ui.ButtonStyle.SECONDARY);
        Ui.PillButton refresh = new Ui.PillButton("刷新", Ui.ButtonStyle.GHOST);
        copy.addActionListener(e -> {
            Ui.copyToClipboard(area.getText());
            copy.setText("已复制");
            javax.swing.Timer timer = new javax.swing.Timer(1400, evt -> copy.setText("复制全文"));
            timer.setRepeats(false);
            timer.start();
        });
        refresh.addActionListener(e -> refresh());
        actions.add(copy);
        actions.add(Ui.gap(8, 0));
        actions.add(refresh);
        header.add(actions, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        area.setEditable(false);
        area.setFont(Theme.mono(11.3f));
        area.setBackground(new Color(0xFA, 0xFB, 0xF9));
        area.setForeground(Theme.TEXT_SOFT);
        area.setBorder(new EmptyBorder(12, 14, 12, 14));
        area.setTabSize(4);
        JPanel areaWrap = Ui.transparent(new BorderLayout());
        areaWrap.setBorder(new EmptyBorder(12, 0, 8, 0));
        areaWrap.add(Ui.scroll(area), BorderLayout.CENTER);
        card.add(areaWrap, BorderLayout.CENTER);

        JPanel footer = Ui.transparent(new BorderLayout());
        JLabel hint = Ui.label("每条边同时挂在两个端点；当前数据为 435 条无向边、870 个边结点。",
                9.8f, Theme.FAINT, Font.PLAIN);
        footer.add(hint, BorderLayout.WEST);
        statsLabel.setHorizontalAlignment(JLabel.RIGHT);
        footer.add(statsLabel, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    public void refresh() {
        area.setText(service.getGraph().adjacencyText());
        area.setCaretPosition(0);
        statsLabel.setText("V = " + service.getGraph().vertexCount()
                + " · E = " + service.getGraph().edgeCount()
                + " · 边结点 = " + service.getGraph().directedEdgeCount());
    }
}
