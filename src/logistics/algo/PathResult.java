package logistics.algo;

import java.util.Collections;
import java.util.List;

/**
 * Dijkstra 最短路径结果：完整顶点序列、总代价、耗时。
 */
public final class PathResult {

    private final int source;
    private final int target;
    private final List<Integer> path;
    private final double totalWeight;
    private final long elapsedNanos;

    public PathResult(int source, int target, List<Integer> path,
                      double totalWeight, long elapsedNanos) {
        this.source = source;
        this.target = target;
        this.path = List.copyOf(path);
        this.totalWeight = totalWeight;
        this.elapsedNanos = elapsedNanos;
    }

    public int getSource() {
        return source;
    }

    public int getTarget() {
        return target;
    }

    public List<Integer> getPath() {
        return Collections.unmodifiableList(path);
    }

    public boolean isReachable() {
        return !path.isEmpty();
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }

    public double getElapsedMillis() {
        return elapsedNanos / 1_000_000.0;
    }

    /** 经过的边数。 */
    public int getEdgeCount() {
        return Math.max(0, path.size() - 1);
    }
}
