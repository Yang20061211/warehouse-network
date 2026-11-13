package logistics.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;

/**
 * 设计说明：为答辩准备的页面内速查。
 */
public final class AboutPanel extends JPanel {

    public AboutPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        Ui.RoundedPanel card = Ui.card();
        card.setBorder(new EmptyBorder(22, 26, 22, 26));

        JEditorPane pane = new JEditorPane();
        pane.setEditorKit(new HTMLEditorKit());
        pane.setContentType("text/html");
        pane.setText(HTML);
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBorder(new EmptyBorder(0, 2, 0, 2));
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setFont(Theme.font(Font.PLAIN, 12.5f));
        pane.setCaretPosition(0);
        card.add(Ui.scroll(pane), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    private static final String HTML = """
            <html><head><style>
            body { font-family: 'PingFang SC'; color: #3B443E; font-size: 13px; line-height: 1.6; }
            h1 { color: #202622; font-size: 22px; margin-bottom: 2px; }
            h2 { color: #202622; font-size: 15px; margin-top: 18px; margin-bottom: 6px; }
            p { margin-top: 4px; margin-bottom: 6px; }
            li { margin-bottom: 3px; }
            .muted { color: #66736A; }
            .accent { color: #234B3D; }
            table { border-collapse: collapse; margin-top: 6px; }
            th { background: #F7F8F6; color: #4B5B51; font-size: 11px; text-align: left; }
            td, th { border: 1px solid #D9DED8; padding: 6px 8px; font-size: 12.5px; }
            code { font-family: Menlo; color: #234B3D; }
            </style></head><body>

            <h1>设计说明</h1>

            <h2>一、场景与目标</h2>
            <p>某物流公司在 30 个城市之间铺设光纤专线：在保证全网连通的前提下让总成本最低，
            并提供任意两仓之间的最短路径查询。系统用
            <b>双向循环链表</b>管理城市，用<b>邻接表</b>存储带权无向图，用
            <b>Prim / Kruskal</b> 求最小生成树，再用 <b>Dijkstra</b> 在生成树拓扑上查询路径。</p>

            <h2>二、三阶段实现</h2>
            <table>
              <tr><th>阶段</th><th>核心结构 / 算法</th><th>实现要点</th></tr>
              <tr><td>1</td><td>带头结点的双向循环链表</td>
                  <td>头结点统一空表边界；尾插 O(1)；删除已定位结点 O(1)；按距离插入排序 O(n²)</td></tr>
              <tr><td>2</td><td>邻接表存储的完全图</td>
                  <td>30 个顶点、435 条无向边、870 个边结点；边权 = 经纬度欧氏距离（2 位小数）</td></tr>
              <tr><td>3</td><td>Prim / Kruskal + Dijkstra</td>
                  <td>两种 MST 总权值必然相同；以 Prim 树建图，Dijkstra 查询任意两城路径</td></tr>
            </table>

            <h2>三、四个关键问题（要点）</h2>
            <ol>
              <li><b>链表如何管理 30 城并支持排序？</b> 用 head 哨兵消除首尾插入/删除的特殊判断；
                  按到基准城市的距离做链表插入排序，先摘链再逐个插入，排序稳定。
                  双向链表删除已持有结点只需改 <code>prev.next</code> 与 <code>next.prev</code>，
                  不必像单链表那样重新找前驱。</li>
              <li><b>邻接矩阵还是邻接表？</b> 30 城完全图共 435 条边：矩阵固定 900 个格子，
                  邻接表 870 个边结点，两者量级接近；一般稀疏图选邻接表、稠密且需要频繁判边选矩阵。
                  本作业明确要求邻接表，且 MST 只保留 29 条边，邻接表在遍历邻边时更省。</li>
              <li><b>Prim 与 Kruskal 的区别？</b> 两者总权值相同。Prim 适合稠密图（O(V²)），
                  Kruskal 适合稀疏图（O(E log E)）。本次 480 城测试中，稠密图 Prim 约 1.60 ms、
                  Kruskal 约 22.74 ms；稀疏图两种算法都在 0.24 ms 以内，具体结果与机器和环境有关。</li>
              <li><b>角色与快速刷新？</b> 管理员可增删改城市，查询员只读查路径与网络信息；
                  每次增删改都会同步重建完全图、MST 与查询缓存，30 城约 1–5 ms。</li>
            </ol>

            <h2>四、最短路径口径</h2>
            <p>按作业要求，默认<b>基于最小生成树的拓扑</b>运行 Dijkstra：树上两点路径唯一，
            结果稳定、便于统一解释。完整网络模式仅用于对比：它可能选出更短的直达路径，
            但那些边不一定在 MST 中。查询内部重复执行 250 次取平均，实测平均耗时在
            <b class="accent">微秒级</b>。具体耗时会随机器和 JVM 状态变化。</p>

            <h2>五、演示账号</h2>
            <p>管理员：<code>admin / admin123</code>　查询员：<code>viewer / viewer123</code><br>
            <span class="muted">退出登录后可切换角色。详细设计、测试数据和运行截图见仓库中的《实验报告》。</span></p>

            </body></html>
            """;
}
