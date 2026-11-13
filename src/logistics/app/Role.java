package logistics.app;

/**
 * 系统角色。至少两种角色是作业要求：
 * 管理员可以增删改城市；查询员只能读网络、查最短路径。
 */
public enum Role {

    ADMIN("管理员", "城市增删改 · 重建图与 MST · 全部查询"),
    VIEWER("查询员", "只读 · 任意两仓最短路径查询");

    private final String label;
    private final String description;

    Role(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean canEdit() {
        return this == ADMIN;
    }
}
