package logistics.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 带头结点的双向循环链表，用来管理城市结点。
 *
 * <pre>
 *        head ⇄ 城市A ⇄ 城市B ⇄ … ⇄ 城市Z
 *         ↑                                  ↓
 *         └──────────────────────────────────┘
 * </pre>
 *
 * <p>head 是哨兵结点，不存数据：
 * <ul>
 *   <li>空表时 head.next == head.prev == head；</li>
 *   <li>尾插时可以直接用 head.prev 找到尾结点，O(1)；</li>
 *   <li>删除时拿到结点 p 后，p.prev 就是前驱，摘链只要改两组指针，O(1)。</li>
 * </ul>
 * 若没有头结点，插入/删除第一个元素时需要单独判断空表、头结点等边界；
 * 有了头结点，所有位置的操作都统一成“在某个结点前插入/删除某个结点”。</p>
 *
 * <p>本类只在界面线程中使用，未做并发控制。</p>
 */
public class CityList {

    /** 链表结点：前驱、后继、数据。 */
    private static final class Node {
        City city;
        Node prev;
        Node next;

        /** 排序时缓存的距离，避免比较过程中反复开方。 */
        double sortKey;

        Node(City city) {
            this.city = city;
        }
    }

    private final Node head = new Node(null);
    private int size;

    public CityList() {
        head.next = head;
        head.prev = head;
    }

    public CityList(List<City> cities) {
        this();
        for (City city : cities) {
            addLast(city);
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 尾插，O(1)。
     * 循环双向链表的头结点 head.prev 永远指向尾结点，因此不必从头遍历。
     */
    public void addLast(City city) {
        if (city == null) {
            throw new IllegalArgumentException("城市不能为空");
        }
        Node node = new Node(city);
        Node tail = head.prev;
        node.prev = tail;
        node.next = head;
        tail.next = node;
        head.prev = node;
        size++;
    }

    /** 查找内部结点，O(n)。 */
    private Node findNode(String name) {
        for (Node p = head.next; p != head; p = p.next) {
            if (p.city.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public City find(String name) {
        Node node = findNode(name);
        return node == null ? null : node.city;
    }

    public boolean contains(String name) {
        return findNode(name) != null;
    }

    /**
     * 按名称删除，O(n)：查找本身需要遍历。
     *
     * <p>但“摘下结点”这一步是 O(1)：p.prev 直接给出前驱。
     * 单链表删除时必须再从头找一次前驱，整体仍是 O(n) 且常数更大；
     * 如果已经持有结点引用（例如在遍历/排序过程中），双向链表能做到 O(1) 删除。</p>
     */
    public boolean remove(String name) {
        Node p = findNode(name);
        if (p == null) {
            return false;
        }
        p.prev.next = p.next;
        p.next.prev = p.prev;
        p.prev = null;
        p.next = null;
        size--;
        return true;
    }

    /** 修改坐标，名称保持不变。 */
    public boolean updatePosition(String name, double lon, double lat) {
        Node p = findNode(name);
        if (p == null) {
            return false;
        }
        p.city = p.city.withPosition(lon, lat);
        return true;
    }

    /** 修改名称和坐标；新名称不能与其它城市重复。 */
    public boolean update(String oldName, City newCity) {
        Node p = findNode(oldName);
        if (p == null) {
            return false;
        }
        if (!oldName.equals(newCity.getName()) && contains(newCity.getName())) {
            return false;
        }
        p.city = newCity;
        return true;
    }

    /**
     * 按各城市到 pivot 城市的欧氏距离升序，就地重排链表。
     *
     * <p>这里演示的是“链表上的插入排序”：
     * 先把所有结点摘下来，再逐个插入到已有序的循环链表中。
     * 时间复杂度 O(n²)，空间复杂度 O(n)；n = 30 时完全够用。
     * 排序是稳定的：距离相同时保持原有先后顺序。</p>
     *
     * @param pivotName 基准城市名称
     */
    public void sortByDistanceFrom(String pivotName) {
        City pivot = find(pivotName);
        if (pivot == null) {
            throw new IllegalArgumentException("找不到基准城市：" + pivotName);
        }
        if (size <= 1) {
            return;
        }

        // 1) 把结点逐个摘下，暂存到数组里；同时断开旧指针
        List<Node> nodes = new ArrayList<>(size);
        Node p = head.next;
        while (p != head) {
            Node next = p.next;
            p.prev = null;
            p.next = null;
            nodes.add(p);
            p = next;
        }
        head.next = head;
        head.prev = head;

        // 2) 预计算排序键，比较时不再重复计算距离
        for (Node node : nodes) {
            node.sortKey = Geo.euclid(pivot, node.city);
        }

        // 3) 插入排序：为每个结点找到第一个 sortKey 更大的位置，插在它前面
        for (Node node : nodes) {
            Node cur = head.next;
            while (cur != head && cur.sortKey <= node.sortKey) {
                cur = cur.next;
            }
            // 插到 cur 之前（cur 可能是 head，即插入到尾部）
            node.prev = cur.prev;
            node.next = cur;
            cur.prev.next = node;
            cur.prev = node;
        }
    }

    /** 按当前链表顺序导出快照，便于建图与界面展示。 */
    public List<City> toList() {
        List<City> result = new ArrayList<>(size);
        for (Node p = head.next; p != head; p = p.next) {
            result.add(p.city);
        }
        return result;
    }

    /** 判断链表是否满足双向循环结构（用于自检）。 */
    public boolean checkStructure() {
        if (head.next == null || head.prev == null) {
            return false;
        }
        if (head.next.prev != head || head.prev.next != head) {
            return false;
        }
        int count = 0;
        Node p = head.next;
        while (p != head) {
            if (p.prev == null || p.next == null || p.next.prev != p) {
                return false;
            }
            count++;
            if (count > size + 1) {
                return false; // 防止环被破坏时死循环
            }
            p = p.next;
        }
        return count == size;
    }
}
