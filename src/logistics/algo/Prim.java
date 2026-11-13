package logistics.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import logistics.model.Graph;

/**
 * Prim 最小生成树算法（朴素 O(V²) 版本）。
 *
 * <p>思路：从 0 号城市出发，每次选择“距离当前树最近”的未访问顶点加入树，
 * 并更新其它顶点到树的距离。顶点数 30，用数组找最小值的常数比堆更小。</p>
 *
 * <p>时间复杂度 O(V²)，适合稠密图；本作业是完全图，正是它的优势场景。</p>
 */
public final class Prim {

    private Prim() {
    }

    public static MstResult run(Graph graph) {
        long start = System.nanoTime();

        int n = graph.vertexCount();
        List<Graph.UndirectedEdge> chosen = new ArrayList<>();
        if (n == 0) {
            return new MstResult(MstResult.Algorithm.PRIM, chosen, 0, System.nanoTime() - start, 0);
        }

        boolean[] inTree = new boolean[n];
        double[] best = new double[n];      // 到当前树的最短边权
        int[] parent = new int[n];          // 该最短边连接树中的哪个顶点
        Arrays.fill(best, Double.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);
        best[0] = 0;

        double total = 0;
        for (int iter = 0; iter < n; iter++) {
            // 1) 找距离树最近、且尚未加入的顶点
            int u = -1;
            for (int i = 0; i < n; i++) {
                if (!inTree[i] && (u == -1 || best[i] < best[u])) {
                    u = i;
                }
            }
            if (u == -1 || Double.isInfinite(best[u])) {
                break; // 图不连通
            }

            // 2) 加入生成树
            inTree[u] = true;
            if (parent[u] != -1) {
                chosen.add(new Graph.UndirectedEdge(parent[u], u, best[u]));
                total += best[u];
            }

            // 3) 用 u 的邻边更新其它顶点到树的距离
            for (Graph.EdgeNode e = graph.firstEdge(u); e != null; e = e.getNext()) {
                int v = e.getTo();
                if (!inTree[v] && e.getWeight() < best[v]) {
                    best[v] = e.getWeight();
                    parent[v] = u;
                }
            }
        }

        total = Math.round(total * 100.0) / 100.0;
        return new MstResult(MstResult.Algorithm.PRIM, chosen, total,
                System.nanoTime() - start, n);
    }
}
