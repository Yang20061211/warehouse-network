package logistics.algo;

/**
 * 并查集：Kruskal 算法判断“加入一条边会不会成环”的核心结构。
 *
 * <p>按秩合并 + 路径压缩，单次操作近似 O(α(n))，α 是反阿克曼函数，
 * 在实际规模下可以看作常数。</p>
 */
public final class UnionFind {

    private final int[] parent;
    private final int[] rank;

    public UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
    }

    /** 查找根结点，同时做路径压缩。 */
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    /**
     * 合并两个集合。
     *
     * @return true 表示原本不在同一集合、合并成功；false 表示会成环
     */
    public boolean union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) {
            return false;
        }
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB;
        } else if (rank[rootA] > rank[rootB]) {
            parent[rootB] = rootA;
        } else {
            parent[rootB] = rootA;
            rank[rootA]++;
        }
        return true;
    }
}
