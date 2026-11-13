package logistics.algo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import logistics.model.City;
import logistics.model.Geo;
import logistics.model.Graph;

/**
 * 算法性能实测工具。
 *
 * <p>计时只包含算法本身：Prim/Kruskal 内部的 nanoTime 差值。
 * 多次运行取平均，减少 JIT 预热和系统噪声的影响；
 * 每组测试前先做几次热身。</p>
 */
public final class Benchmark {

    private Benchmark() {
    }

    /** 当前图的 Prim / Kruskal 平均耗时。 */
    public static final class MstStats {
        public final double primAvgMs;
        public final double kruskalAvgMs;
        public final double primTotal;
        public final double kruskalTotal;
        public final int runs;

        MstStats(double primAvgMs, double kruskalAvgMs,
                 double primTotal, double kruskalTotal, int runs) {
            this.primAvgMs = primAvgMs;
            this.kruskalAvgMs = kruskalAvgMs;
            this.primTotal = primTotal;
            this.kruskalTotal = kruskalTotal;
            this.runs = runs;
        }

        public String faster() {
            double diff = Math.abs(primAvgMs - kruskalAvgMs);
            if (diff < 0.0005) {
                return "基本持平";
            }
            return primAvgMs < kruskalAvgMs ? "Prim 更快" : "Kruskal 更快";
        }
    }

    /** 当前图全点对 Dijkstra 的统计。 */
    public static final class PathStats {
        public final int queries;
        public final double avgMs;
        public final double maxMs;

        PathStats(int queries, double avgMs, double maxMs) {
            this.queries = queries;
            this.avgMs = avgMs;
            this.maxMs = maxMs;
        }
    }

    /** 一组规模扩展测试结果。 */
    public static final class Row {
        public final int vertices;
        public final int edges;
        public final boolean dense;
        public final double primAvgMs;
        public final double kruskalAvgMs;

        Row(int vertices, int edges, boolean dense, double primAvgMs, double kruskalAvgMs) {
            this.vertices = vertices;
            this.edges = edges;
            this.dense = dense;
            this.primAvgMs = primAvgMs;
            this.kruskalAvgMs = kruskalAvgMs;
        }

        public String faster() {
            double diff = Math.abs(primAvgMs - kruskalAvgMs);
            if (diff < 0.0005) {
                return "持平";
            }
            return primAvgMs < kruskalAvgMs ? "Prim" : "Kruskal";
        }
    }

    public static MstStats measureMst(Graph graph, int runs) {
        int reps = Math.max(1, runs);
        MstResult prim = Prim.run(graph);
        MstResult kruskal = Kruskal.run(graph);
        checkSameTotal(prim, kruskal);

        // 热身，让 JIT 编译热点代码
        for (int i = 0; i < 5; i++) {
            Prim.run(graph);
            Kruskal.run(graph);
        }

        long primNanos = 0;
        long kruskalNanos = 0;
        for (int i = 0; i < reps; i++) {
            MstResult p = Prim.run(graph);
            MstResult k = Kruskal.run(graph);
            primNanos += p.getElapsedNanos();
            kruskalNanos += k.getElapsedNanos();
            checkSameTotal(p, k);
        }
        return new MstStats(primNanos / 1_000_000.0 / reps,
                kruskalNanos / 1_000_000.0 / reps,
                prim.getTotalWeight(), kruskal.getTotalWeight(), reps);
    }

    public static PathStats measureDijkstraAllPairs(Graph graph, int rounds) {
        int reps = Math.max(1, rounds);
        int n = graph.vertexCount();
        int pairs = n * (n - 1) / 2;

        // 热身
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Dijkstra.run(graph, i, j);
            }
        }

        long totalNanos = 0;
        long maxNanos = 0;
        int queries = 0;
        for (int round = 0; round < reps; round++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    PathResult result = Dijkstra.run(graph, i, j);
                    totalNanos += result.getElapsedNanos();
                    maxNanos = Math.max(maxNanos, result.getElapsedNanos());
                    queries++;
                }
            }
        }
        return new PathStats(queries, totalNanos / 1_000_000.0 / queries,
                maxNanos / 1_000_000.0);
    }

    /**
     * 规模扩展测试：同一规模下跑若干次 Prim 与 Kruskal，返回平均耗时。
     *
     * @param sizes 顶点规模，例如 30, 60, 120, 240, 480
     * @param dense true = 完全图（稠密）；false = 平均度约 6 的稀疏连通图
     * @param runs  每个规模重复次数
     */
    public static List<Row> scalability(int[] sizes, boolean dense, int runs, long seed) {
        List<Row> rows = new ArrayList<>();
        for (int size : sizes) {
            Graph graph = syntheticGraph(size, dense, seed + size);
            Prim.run(graph);
            Kruskal.run(graph);

            long primNanos = 0;
            long kruskalNanos = 0;
            for (int i = 0; i < Math.max(1, runs); i++) {
                MstResult p = Prim.run(graph);
                MstResult k = Kruskal.run(graph);
                primNanos += p.getElapsedNanos();
                kruskalNanos += k.getElapsedNanos();
                checkSameTotal(p, k);
            }
            rows.add(new Row(size, graph.edgeCount(), dense,
                    primNanos / 1_000_000.0 / Math.max(1, runs),
                    kruskalNanos / 1_000_000.0 / Math.max(1, runs)));
        }
        return rows;
    }

    private static void checkSameTotal(MstResult a, MstResult b) {
        if (Math.abs(a.getTotalWeight() - b.getTotalWeight()) > 1e-6) {
            throw new IllegalStateException("Prim 与 Kruskal 总权值不一致："
                    + a.getTotalWeight() + " vs " + b.getTotalWeight());
        }
    }

    /** 生成用于压力测试的合成城市图（不写入数据文件）。 */
    private static Graph syntheticGraph(int n, boolean dense, long seed) {
        Random random = new Random(seed);
        List<City> cities = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double lon = Math.round((73 + random.nextDouble() * 62) * 100.0) / 100.0;
            double lat = Math.round((18 + random.nextDouble() * 36) * 100.0) / 100.0;
            cities.add(new City("S" + i, lon, lat));
        }
        if (dense) {
            return Graph.completeGraph(cities);
        }

        List<Graph.UndirectedEdge> edges = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        // 先加一棵随机生成树，保证连通
        for (int i = 1; i < n; i++) {
            int parent = random.nextInt(i);
            addEdge(edges, seen, cities, parent, i);
        }
        // 再按概率补充边，平均度约为 6
        double probability = Math.min(1.0, 6.0 / Math.max(1, n - 1));
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (random.nextDouble() < probability) {
                    addEdge(edges, seen, cities, i, j);
                }
            }
        }
        return Graph.fromEdges(cities, edges);
    }

    private static void addEdge(List<Graph.UndirectedEdge> edges, Set<Long> seen,
                                List<City> cities, int a, int b) {
        int u = Math.min(a, b);
        int v = Math.max(a, b);
        long key = (long) u * 1_000_000L + v;
        if (seen.add(key)) {
            edges.add(new Graph.UndirectedEdge(u, v, Geo.weight(cities.get(u), cities.get(v))));
        }
    }
}
