package logistics.algo;

import java.util.Collections;
import java.util.List;
import logistics.model.Graph;

/**
 * 最小生成树的计算结果：选中的边、总权值、耗时。
 */
public final class MstResult {

    public enum Algorithm {
        PRIM("Prim"),
        KRUSKAL("Kruskal");

        private final String label;

        Algorithm(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Algorithm algorithm;
    private final List<Graph.UndirectedEdge> edges;
    private final double totalWeight;
    private final long elapsedNanos;
    private final int vertexCount;

    public MstResult(Algorithm algorithm, List<Graph.UndirectedEdge> edges,
                     double totalWeight, long elapsedNanos, int vertexCount) {
        this.algorithm = algorithm;
        this.edges = List.copyOf(edges);
        this.totalWeight = totalWeight;
        this.elapsedNanos = elapsedNanos;
        this.vertexCount = vertexCount;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public List<Graph.UndirectedEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }

    /** 单次运行耗时，毫秒。 */
    public double getElapsedMillis() {
        return elapsedNanos / 1_000_000.0;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    /** 是否覆盖了所有顶点（边数 = V-1）。 */
    public boolean isSpanningTree() {
        return vertexCount <= 1 || edges.size() == vertexCount - 1;
    }
}
