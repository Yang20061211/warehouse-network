package logistics.algo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import logistics.model.Graph;

/**
 * Kruskal 最小生成树算法。
 *
 * <p>思路：把所有边按权值升序排列，依次尝试加入；
 * 用并查集判断两个端点是否已经连通，不连通才加入，直到选够 V-1 条边。</p>
 *
 * <p>时间复杂度 O(E log E)，主要开销在排序；适合稀疏图。</p>
 */
public final class Kruskal {

    private Kruskal() {
    }

    public static MstResult run(Graph graph) {
        long start = System.nanoTime();

        int n = graph.vertexCount();
        List<Graph.UndirectedEdge> all = new ArrayList<>(graph.edges());
        all.sort(Comparator
                .comparingDouble(Graph.UndirectedEdge::getWeight)
                .thenComparingInt(Graph.UndirectedEdge::getU)
                .thenComparingInt(Graph.UndirectedEdge::getV));

        UnionFind uf = new UnionFind(n);
        List<Graph.UndirectedEdge> chosen = new ArrayList<>(Math.max(0, n - 1));
        double total = 0;
        int need = Math.max(0, n - 1);

        for (Graph.UndirectedEdge edge : all) {
            if (chosen.size() == need) {
                break;
            }
            if (uf.union(edge.getU(), edge.getV())) {
                chosen.add(edge);
                total += edge.getWeight();
            }
        }

        total = Math.round(total * 100.0) / 100.0;
        return new MstResult(MstResult.Algorithm.KRUSKAL, chosen, total,
                System.nanoTime() - start, n);
    }
}
