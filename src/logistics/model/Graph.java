package logistics.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 带权无向图，采用邻接表存储。
 *
 * <p>每个顶点 {@link Vertex} 保存城市和一条“出边单链表”的头指针；
 * 无向边 (u, v, w) 会同时出现在 u 与 v 的边链表中，各存一次。
 * 30 个城市的完全图有 435 条无向边、870 个边结点。</p>
 *
 * <p>说明：图中把经纬度当作平面坐标，边权是欧氏距离（保留 2 位小数）。</p>
 */
public class Graph {

    /** 边结点：邻接点下标、权值、下一条边。 */
    public static final class EdgeNode {
        private final int to;
        private final double weight;
        private final EdgeNode next;

        EdgeNode(int to, double weight, EdgeNode next) {
            this.to = to;
            this.weight = weight;
            this.next = next;
        }

        public int getTo() {
            return to;
        }

        public double getWeight() {
            return weight;
        }

        public EdgeNode getNext() {
            return next;
        }
    }

    /** 顶点：城市 + 边链表头指针。 */
    public static final class Vertex {
        private final City city;
        private EdgeNode first;

        Vertex(City city) {
            this.city = city;
        }

        public City getCity() {
            return city;
        }

        public EdgeNode getFirst() {
            return first;
        }
    }

    /** 一条无向边（u &lt; v），供 Kruskal 排序与 MST 结果展示使用。 */
    public static final class UndirectedEdge {
        private final int u;
        private final int v;
        private final double weight;

        public UndirectedEdge(int u, int v, double weight) {
            if (u < 0 || v < 0 || u == v) {
                throw new IllegalArgumentException("非法边: " + u + "-" + v);
            }
            if (u < v) {
                this.u = u;
                this.v = v;
            } else {
                this.u = v;
                this.v = u;
            }
            this.weight = weight;
        }

        public int getU() {
            return u;
        }

        public int getV() {
            return v;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return u + " — " + v + " (" + String.format(Locale.ROOT, "%.2f", weight) + ")";
        }
    }

    private final List<Vertex> vertices = new ArrayList<>();
    private final Map<String, Integer> indexOf = new HashMap<>();
    private final List<UndirectedEdge> edges = new ArrayList<>();
    private int directedEdgeCount;

    private Graph(List<City> cities) {
        for (City city : cities) {
            if (indexOf.containsKey(city.getName())) {
                throw new IllegalArgumentException("城市名称重复：" + city.getName());
            }
            indexOf.put(city.getName(), vertices.size());
            vertices.add(new Vertex(city));
        }
    }

    /** 建立完全图：任意两个城市之间都有一条边。 */
    public static Graph completeGraph(List<City> cities) {
        Graph graph = new Graph(cities);
        int n = cities.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double weight = Geo.weight(cities.get(i), cities.get(j));
                graph.addUndirectedEdge(i, j, weight);
            }
        }
        return graph;
    }

    /**
     * 用指定的边子集建图。
     * 顶点集合与下标和传入的 cities 一致，便于把 MST 结果直接交给 Dijkstra。
     */
    public static Graph fromEdges(List<City> cities, List<UndirectedEdge> subset) {
        Graph graph = new Graph(cities);
        for (UndirectedEdge edge : subset) {
            graph.addUndirectedEdge(edge.u, edge.v, edge.weight);
        }
        return graph;
    }

    private void addUndirectedEdge(int u, int v, double weight) {
        vertices.get(u).first = new EdgeNode(v, weight, vertices.get(u).first);
        vertices.get(v).first = new EdgeNode(u, weight, vertices.get(v).first);
        edges.add(new UndirectedEdge(u, v, weight));
        directedEdgeCount += 2;
    }

    public int vertexCount() {
        return vertices.size();
    }

    /** 无向边数。 */
    public int edgeCount() {
        return edges.size();
    }

    /** 邻接表中的边结点总数（无向图 = 2 × 边数）。 */
    public int directedEdgeCount() {
        return directedEdgeCount;
    }

    public City city(int index) {
        return vertices.get(index).city;
    }

    public Vertex vertex(int index) {
        return vertices.get(index);
    }

    public EdgeNode firstEdge(int index) {
        return vertices.get(index).first;
    }

    /** 城市名 → 下标；找不到返回 -1。 */
    public int indexOf(String cityName) {
        Integer index = indexOf.get(cityName);
        return index == null ? -1 : index;
    }

    public int degree(int index) {
        int degree = 0;
        for (EdgeNode e = firstEdge(index); e != null; e = e.getNext()) {
            degree++;
        }
        return degree;
    }

    /** 所有无向边（每条只出现一次），供 Kruskal / MST 展示使用。 */
    public List<UndirectedEdge> edges() {
        return Collections.unmodifiableList(edges);
    }

    /** 生成完整的邻接表文本示意图。 */
    public String adjacencyText() {
        StringBuilder sb = new StringBuilder();
        sb.append("邻接表 · 带权无向图\n");
        sb.append(String.format(Locale.ROOT,
                "顶点数 %d    无向边数 %d    有向边结点 %d%n%n",
                vertexCount(), edgeCount(), directedEdgeCount));
        for (int i = 0; i < vertices.size(); i++) {
            City city = vertices.get(i).city;
            sb.append(String.format(Locale.ROOT, "[%2d] %s (%.2f, %.2f)  度=%d%n",
                    i, city.getName(), city.getLon(), city.getLat(), degree(i)));
            List<String> parts = new ArrayList<>();
            for (EdgeNode e = firstEdge(i); e != null; e = e.getNext()) {
                parts.add(String.format(Locale.ROOT, "%s %.2f", vertices.get(e.to).city.getName(), e.weight));
            }
            if (parts.isEmpty()) {
                sb.append("      ∥ 空链表\n");
                continue;
            }
            String indent = "      ";
            StringBuilder line = new StringBuilder(indent).append("→ ");
            for (int k = 0; k < parts.size(); k++) {
                String token = parts.get(k) + (k + 1 < parts.size() ? " → " : "");
                if (line.length() + token.length() > 96 && line.length() > indent.length() + 2) {
                    sb.append(line).append('\n');
                    line = new StringBuilder(indent).append("  ").append(token);
                } else {
                    line.append(token);
                }
            }
            sb.append(line).append('\n');
            sb.append('\n');
        }
        return sb.toString();
    }
}
