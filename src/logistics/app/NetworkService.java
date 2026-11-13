package logistics.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import logistics.algo.Dijkstra;
import logistics.algo.Kruskal;
import logistics.algo.MstResult;
import logistics.algo.PathResult;
import logistics.algo.Prim;
import logistics.model.City;
import logistics.model.CityList;
import logistics.model.Graph;

/**
 * 应用的状态中心：持有链表、当前图、两棵 MST 和 MST 邻接图。
 *
 * <p>任何城市变更都会调用 {@link #rebuild()}：
 * 重新生成完全图 → 重跑 Prim 与 Kruskal → 用 Prim 的树建一张只含 MST 边的图，
 * 供 Dijkstra 查询。30 个顶点下整轮重建通常只需几毫秒，
 * 满足“增删城市后快速更新”的要求。</p>
 */
public class NetworkService {

    /** 最短路径查询统计：多次运行的总耗时、平均值与最大值。 */
    public static final class QueryStats {
        public final PathResult result;
        public final double averageMillis;
        public final double maxMillis;
        public final int runs;

        QueryStats(PathResult result, double averageMillis, double maxMillis, int runs) {
            this.result = result;
            this.averageMillis = averageMillis;
            this.maxMillis = maxMillis;
            this.runs = runs;
        }
    }

    private final Path dataFile;
    private final List<Runnable> listeners = new ArrayList<>();

    private CityList cityList;
    private Graph graph;
    private Graph mstGraph;
    private MstResult primResult;
    private MstResult kruskalResult;
    private long rebuildNanos;
    private String warning;

    public NetworkService(Path dataFile) {
        this.dataFile = Objects.requireNonNull(dataFile, "dataFile");
        load();
        rebuild();
        // 预热 JIT：首次调用包含类加载/解释执行，第二次的耗时更接近稳态，
        // 避免界面刚启动时把“冷启动”误报成正常重建耗时。
        rebuild();
    }

    private void load() {
        List<City> loaded = new ArrayList<>();
        if (Files.exists(dataFile)) {
            try {
                for (String raw : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    if (parts.length < 3) {
                        continue;
                    }
                    try {
                        double lon = Double.parseDouble(parts[1]);
                        double lat = Double.parseDouble(parts[2]);
                        if (!validLonLat(lon, lat)) {
                            continue;
                        }
                        City city = new City(parts[0], lon, lat);
                        boolean duplicate = loaded.stream()
                                .anyMatch(c -> c.getName().equals(city.getName()));
                        if (!duplicate) {
                            loaded.add(city);
                        }
                    } catch (NumberFormatException ignored) {
                        // 跳过格式错误的行，保证系统仍能启动
                    }
                }
            } catch (IOException e) {
                warning = "读取数据文件失败，已使用内置数据：" + e.getMessage();
            }
        }
        if (loaded.size() < 2) {
            loaded = new ArrayList<>(DefaultData.cities());
            warning = "数据文件缺失或城市不足 2 个，已载入内置 30 城";
        }
        cityList = new CityList(loaded);
    }

    private void save() throws IOException {
        Path parent = dataFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# 城市名 经度 纬度（UTF-8）\n");
        for (City city : cityList.toList()) {
            sb.append(String.format(Locale.ROOT, "%s %.2f %.2f%n",
                    city.getName(), city.getLon(), city.getLat()));
        }
        Files.writeString(dataFile, sb.toString(), StandardCharsets.UTF_8);
    }

    private void saveQuietly() {
        try {
            save();
        } catch (IOException e) {
            warning = "数据保存失败：" + e.getMessage();
            System.err.println(warning);
        }
    }

    /** 重建图、Prim、Kruskal 与 MST 邻接图，并记录总耗时。 */
    public void rebuild() {
        long start = System.nanoTime();
        List<City> cities = cityList.toList();
        graph = Graph.completeGraph(cities);
        primResult = Prim.run(graph);
        kruskalResult = Kruskal.run(graph);
        mstGraph = Graph.fromEdges(cities, primResult.getEdges());
        rebuildNanos = System.nanoTime() - start;
    }

    public void addCity(City city) {
        validateFields(city);
        if (cityList.contains(city.getName())) {
            throw new IllegalArgumentException("城市「" + city.getName() + "」已经存在");
        }
        cityList.addLast(city);
        rebuild();
        saveQuietly();
        notifyListeners();
    }

    public boolean removeCity(String name) {
        if (cityList.size() <= 2) {
            throw new IllegalStateException("至少保留 2 个城市，才能计算距离与路径");
        }
        if (!cityList.remove(name)) {
            return false;
        }
        rebuild();
        saveQuietly();
        notifyListeners();
        return true;
    }

    public void updateCity(String oldName, City newCity) {
        validateFields(newCity);
        if (!cityList.contains(oldName)) {
            throw new IllegalArgumentException("城市「" + oldName + "」不存在");
        }
        if (!oldName.equals(newCity.getName()) && cityList.contains(newCity.getName())) {
            throw new IllegalArgumentException("城市「" + newCity.getName() + "」已经存在");
        }
        cityList.update(oldName, newCity);
        rebuild();
        saveQuietly();
        notifyListeners();
    }

    /** 按到 pivot 的距离就地重排双向循环链表，然后重建图与 MST。 */
    public void sortByDistance(String pivot) {
        cityList.sortByDistanceFrom(pivot);
        rebuild();
        saveQuietly();
        notifyListeners();
    }

    /** 恢复内置的 30 城数据。 */
    public void resetDefaults() {
        cityList = new CityList(new ArrayList<>(DefaultData.cities()));
        rebuild();
        saveQuietly();
        notifyListeners();
    }

    private void validateFields(City city) {
        if (city == null) {
            throw new IllegalArgumentException("城市不能为空");
        }
        String name = city.getName();
        if (name.isBlank()) {
            throw new IllegalArgumentException("城市名称不能为空");
        }
        if (name.matches(".*\\s+.*")) {
            throw new IllegalArgumentException("城市名称中不能包含空格");
        }
        if (!validLonLat(city.getLon(), city.getLat())) {
            throw new IllegalArgumentException("坐标不合法：经度应在 -180~180，纬度应在 -90~90");
        }
    }

    private static boolean validLonLat(double lon, double lat) {
        return Double.isFinite(lon) && Double.isFinite(lat)
                && lon >= -180 && lon <= 180
                && lat >= -90 && lat <= 90;
    }

    /**
     * 任意两城最短路径。
     *
     * @param fullNetwork true 用完整网络；false 用 MST 拓扑（作业要求）
     * @param repeats 重复运行次数，用于计算平均耗时；至少 1 次
     */
    public QueryStats queryPath(String fromName, String toName,
                                boolean fullNetwork, int repeats) {
        int from = graph.indexOf(fromName);
        int to = graph.indexOf(toName);
        if (from < 0 || to < 0) {
            throw new IllegalArgumentException("起点或终点不存在");
        }
        Graph base = fullNetwork ? graph : mstGraph;

        // 热身一次，避免把 JIT 编译时间算进平均值
        Dijkstra.run(base, from, to);

        int reps = Math.max(1, repeats);
        PathResult last = null;
        long total = 0;
        long max = 0;
        for (int i = 0; i < reps; i++) {
            last = Dijkstra.run(base, from, to);
            total += last.getElapsedNanos();
            max = Math.max(max, last.getElapsedNanos());
        }
        return new QueryStats(last, total / 1_000_000.0 / reps,
                max / 1_000_000.0, reps);
    }

    public List<City> snapshotCities() {
        return cityList.toList();
    }

    public CityList getCityList() {
        return cityList;
    }

    public Graph getGraph() {
        return graph;
    }

    public Graph getMstGraph() {
        return mstGraph;
    }

    public MstResult getPrimResult() {
        return primResult;
    }

    public MstResult getKruskalResult() {
        return kruskalResult;
    }

    public int cityCount() {
        return cityList.size();
    }

    public double getRebuildMillis() {
        return rebuildNanos / 1_000_000.0;
    }

    public Path getDataFile() {
        return dataFile;
    }

    /** 取出并清除一条警告（保存失败等）。 */
    public String consumeWarning() {
        String value = warning;
        warning = null;
        return value;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
