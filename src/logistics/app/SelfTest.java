package logistics.app;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import logistics.algo.Benchmark;
import logistics.algo.MstResult;
import logistics.algo.PathResult;
import logistics.model.City;
import logistics.model.CityList;
import logistics.model.Geo;
import logistics.model.Graph;

/**
 * 命令行自检 / 报告数据生成器。
 *
 * <p>运行方式：{@code java -cp out logistics.app.SelfTest}
 * 输出三个阶段的结构、算法结果、路径样例和性能实测表，
 * 实验报告里的数字直接来自这份输出。</p>
 */
public final class SelfTest {

    private SelfTest() {
    }

    public static void main(String[] args) {
        Path data = Paths.get(System.getProperty("user.dir"), "data", "cities.txt");
        NetworkService service = new NetworkService(data);
        run(service);
    }

    public static void run(NetworkService service) {
        printStageOne(service);
        printStageTwo(service);
        printStageThree(service);
        printPerformance(service);
    }

    // ─────────────────────────── 阶段一 ───────────────────────────

    private static void printStageOne(NetworkService service) {
        System.out.println("========== 第一阶段：双向循环链表 ==========");
        CityList list = new CityList(service.snapshotCities());
        System.out.printf(Locale.ROOT, "载入城市数：%d%n", list.size());
        System.out.println("双向循环结构自检：" + (list.checkStructure() ? "通过" : "失败"));

        City beijing = list.find("北京");
        System.out.printf(Locale.ROOT, "查找「北京」：%s (%.2f, %.2f)%n",
                beijing == null ? "未找到" : beijing.getName(),
                beijing == null ? 0 : beijing.getLon(),
                beijing == null ? 0 : beijing.getLat());

        City test = new City("测试城", 112.00, 33.00);
        list.addLast(test);
        System.out.println("新增「测试城」后城市数：" + list.size());
        list.updatePosition("测试城", 113.50, 34.20);
        City updated = list.find("测试城");
        System.out.printf(Locale.ROOT, "修改「测试城」坐标：%s (%.2f, %.2f)%n",
                updated.getName(), updated.getLon(), updated.getLat());
        System.out.println("删除「测试城」：" + (list.remove("测试城") ? "成功" : "失败")
                + "，剩余城市数：" + list.size());
        System.out.println("删除后结构自检：" + (list.checkStructure() ? "通过" : "失败"));

        list.sortByDistanceFrom("北京");
        System.out.println();
        System.out.println("按到「北京」的欧氏距离升序排序（链表就地重排）：");
        System.out.println("序号  城市        经度     纬度     到北京距离");
        City pivot = list.find("北京");
        List<City> sorted = list.toList();
        for (int i = 0; i < sorted.size(); i++) {
            City city = sorted.get(i);
            System.out.printf(Locale.ROOT, "%3d   %-8s %8.2f %8.2f %12.2f%n",
                    i + 1, city.getName(), city.getLon(), city.getLat(),
                    Geo.euclid(pivot, city));
        }
        System.out.println("排序后结构自检：" + (list.checkStructure() ? "通过" : "失败"));
        System.out.println();
    }

    // ─────────────────────────── 阶段二 ───────────────────────────

    private static void printStageTwo(NetworkService service) {
        System.out.println("========== 第二阶段：邻接表存储的带权无向图 ==========");
        Graph graph = service.getGraph();
        System.out.printf(Locale.ROOT, "顶点数：%d，无向边数：%d，邻接表边结点数：%d%n",
                graph.vertexCount(), graph.edgeCount(), graph.directedEdgeCount());
        System.out.println();
        System.out.println(graph.adjacencyText());
    }

    // ─────────────────────────── 阶段三 ───────────────────────────

    private static void printStageThree(NetworkService service) {
        System.out.println("========== 第三阶段：MST 与最短路径 ==========");
        MstResult prim = service.getPrimResult();
        MstResult kruskal = service.getKruskalResult();
        Graph graph = service.getGraph();

        System.out.printf(Locale.ROOT, "Prim    总权值：%.2f，边数：%d，单次耗时：%s%n",
                prim.getTotalWeight(), prim.getEdges().size(), formatMs(prim.getElapsedMillis()));
        printEdges(graph, prim, "Prim 选中的边");

        System.out.printf(Locale.ROOT, "Kruskal 总权值：%.2f，边数：%d，单次耗时：%s%n",
                kruskal.getTotalWeight(), kruskal.getEdges().size(),
                formatMs(kruskal.getElapsedMillis()));
        printEdges(graph, kruskal, "Kruskal 选中的边");

        System.out.println("两种算法总权值是否一致："
                + (Math.abs(prim.getTotalWeight() - kruskal.getTotalWeight()) < 1e-6 ? "一致" : "不一致"));
        System.out.printf(Locale.ROOT, "（按 1 度 ≈ 111 km 粗略换算，总成本约 %.1f km）%n%n",
                prim.getTotalWeight() * Geo.KM_PER_DEGREE);

        System.out.println("Dijkstra 查询示例（基于 MST 拓扑）：");
        String[][] pairs = {
                {"北京", "乌鲁木齐"},
                {"广州", "哈尔滨"},
                {"拉萨", "上海"},
                {"成都", "厦门"}
        };
        for (String[] pair : pairs) {
            NetworkService.QueryStats stats = service.queryPath(pair[0], pair[1], false, 200);
            PathResult result = stats.result;
            System.out.printf(Locale.ROOT, "%s → %s：%s%n",
                    pair[0], pair[1], pathText(graph, result));
            System.out.printf(Locale.ROOT, "    总代价 %.2f，%d 段；%d 次平均 %s，最慢 %s%n",
                    result.getTotalWeight(), result.getEdgeCount(), stats.runs,
                    formatMs(stats.averageMillis), formatMs(stats.maxMillis));

            NetworkService.QueryStats full = service.queryPath(pair[0], pair[1], true, 200);
            System.out.printf(Locale.ROOT, "    完整网络对比：%s，总代价 %.2f%n%n",
                    pathText(graph, full.result), full.result.getTotalWeight());
        }

        Benchmark.PathStats mstStats = Benchmark.measureDijkstraAllPairs(service.getMstGraph(), 20);
        System.out.printf(Locale.ROOT,
                "全点对查询（MST，%d 组）：平均 %s，最慢 %s%n",
                mstStats.queries, formatMs(mstStats.avgMs), formatMs(mstStats.maxMs));
        Benchmark.PathStats fullStats = Benchmark.measureDijkstraAllPairs(graph, 20);
        System.out.printf(Locale.ROOT,
                "全点对查询（完整网络，%d 组）：平均 %s，最慢 %s%n%n",
                fullStats.queries, formatMs(fullStats.avgMs), formatMs(fullStats.maxMs));
    }

    private static void printEdges(Graph graph, MstResult result, String title) {
        System.out.println(title + "：");
        List<Graph.UndirectedEdge> edges = result.getEdges();
        for (int i = 0; i < edges.size(); i++) {
            Graph.UndirectedEdge edge = edges.get(i);
            System.out.printf(Locale.ROOT, "  %2d. %-8s — %-8s %6.2f%n",
                    i + 1,
                    graph.city(edge.getU()).getName(),
                    graph.city(edge.getV()).getName(),
                    edge.getWeight());
        }
        System.out.println();
    }

    private static String pathText(Graph graph, PathResult result) {
        if (!result.isReachable()) {
            return "不可达";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.getPath().size(); i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(graph.city(result.getPath().get(i)).getName());
        }
        return sb.toString();
    }

    // ─────────────────────────── 性能实测 ───────────────────────────

    private static void printPerformance(NetworkService service) {
        System.out.println("========== 性能实测 ==========");
        Graph graph = service.getGraph();
        Benchmark.MstStats current = Benchmark.measureMst(graph, 300);
        System.out.printf(Locale.ROOT,
                "当前 %d 城 %d 条边（%d 次平均）%n", graph.vertexCount(), graph.edgeCount(), current.runs);
        System.out.printf(Locale.ROOT, "  Prim    %s%n", formatMs(current.primAvgMs));
        System.out.printf(Locale.ROOT, "  Kruskal %s%n", formatMs(current.kruskalAvgMs));
        System.out.println("  结论：" + current.faster());
        System.out.println();

        // 重建性能：模拟“增/删城市后快速更新”的核心路径
        for (int i = 0; i < 5; i++) {
            service.rebuild();
        }
        long rebuildTotal = 0;
        int rebuildRuns = 300;
        for (int i = 0; i < rebuildRuns; i++) {
            long t0 = System.nanoTime();
            service.rebuild();
            rebuildTotal += System.nanoTime() - t0;
        }
        System.out.printf(Locale.ROOT,
                "  ［重建］完全图 + Prim + Kruskal + MST 缓存：%s（%d 次平均）%n%n",
                formatMs(rebuildTotal / 1_000_000.0 / rebuildRuns), rebuildRuns);

        int[] sizes = {30, 60, 120, 240, 480};
        System.out.println("规模扩展测试（每种规模 10 次平均）");
        for (boolean dense : new boolean[]{true, false}) {
            System.out.println(dense ? "— 稠密图（完全图）—" : "— 稀疏图（平均度约 6）—");
            System.out.println("顶点数    边数      Prim         Kruskal      更快");
            List<Benchmark.Row> rows = Benchmark.scalability(sizes, dense, 10, 2026L);
            for (Benchmark.Row row : rows) {
                System.out.printf(Locale.ROOT, "%5d  %7d  %10s  %10s  %s%n",
                        row.vertices, row.edges,
                        formatMsPlain(row.primAvgMs), formatMsPlain(row.kruskalAvgMs),
                        row.faster());
            }
            System.out.println();
        }
    }

    private static String formatMs(double ms) {
        if (ms < 1) {
            return String.format(Locale.ROOT, "%.3f ms (%.0f µs)", ms, ms * 1000);
        }
        return String.format(Locale.ROOT, "%.3f ms", ms);
    }

    private static String formatMsPlain(double ms) {
        if (ms < 1) {
            return String.format(Locale.ROOT, "%.4f ms", ms);
        }
        return String.format(Locale.ROOT, "%.2f ms", ms);
    }
}
