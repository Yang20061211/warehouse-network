package logistics.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import logistics.model.Graph;

/**
 * Dijkstra 单源最短路径算法（朴素 O(V²) 版本）。
 *
 * <p>作业要求：基于最小生成树的拓扑结构查询任意两城之间的最短路径。
 * 因此调用方通常传入“只含 MST 边”的图；树上任意两点路径唯一，
 * Dijkstra 在树上仍然能正确工作。界面也提供了“完整网络”模式作对比。</p>
 *
 * <p>因为边权非负，用数组每次挑选未确定顶点中距离最小者即可；
 * 30 个顶点时这比堆实现更简单，实测单次查询在微秒级。</p>
 */
public final class Dijkstra {

    private static final double EPS = 1e-12;

    private Dijkstra() {
    }

    public static PathResult run(Graph graph, int source, int target) {
        long start = System.nanoTime();

        int n = graph.vertexCount();
        double[] dist = new double[n];
        int[] prev = new int[n];
        boolean[] done = new boolean[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);
        dist[source] = 0;

        for (int iter = 0; iter < n; iter++) {
            // 1) 在未确定的顶点中选 dist 最小者
            int u = -1;
            for (int i = 0; i < n; i++) {
                if (!done[i] && dist[i] < Double.POSITIVE_INFINITY
                        && (u == -1 || dist[i] < dist[u])) {
                    u = i;
                }
            }
            if (u == -1) {
                break; // 剩余顶点不可达
            }
            done[u] = true;
            if (u == target) {
                break; // 目标已确定，可以提前结束
            }

            // 2) 松弛 u 的所有邻边
            for (Graph.EdgeNode e = graph.firstEdge(u); e != null; e = e.getNext()) {
                int v = e.getTo();
                if (!done[v] && dist[u] + e.getWeight() < dist[v] - EPS) {
                    dist[v] = dist[u] + e.getWeight();
                    prev[v] = u;
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        if (dist[target] < Double.POSITIVE_INFINITY) {
            for (int v = target; v != -1; v = prev[v]) {
                path.add(0, v);
            }
        }
        double total = dist[target] < Double.POSITIVE_INFINITY
                ? Math.round(dist[target] * 100.0) / 100.0
                : Double.POSITIVE_INFINITY;

        return new PathResult(source, target, path, total, System.nanoTime() - start);
    }
}
