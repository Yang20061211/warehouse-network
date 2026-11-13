package logistics.app;

/**
 * 系统角色。至少两种角色是作业要求：
 * 管理员可以增删改城市；查询员只能读网络、查最短路径。
 */
public enum Role {

    ADMIN("管理员"),
    VIEWER("查询员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canEdit() {
        return this == ADMIN;
    }
}
