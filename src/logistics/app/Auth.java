package logistics.app;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示用账号系统。
 *
 * <p>课程设计只需要体现“角色隔离”，因此账号写死在代码里；
 * 真实系统应使用数据库存储加盐哈希后的密码。</p>
 */
public final class Auth {

    private static final Map<String, String> PASSWORDS = new HashMap<>();
    private static final Map<String, User> USERS = new HashMap<>();

    static {
        register(new User("admin", "张经理", Role.ADMIN), "admin123");
        register(new User("viewer", "李调度", Role.VIEWER), "viewer123");
    }

    private Auth() {
    }

    private static void register(User user, String password) {
        USERS.put(user.getUsername(), user);
        PASSWORDS.put(user.getUsername(), password);
    }

    /** 登录成功返回用户，失败返回 null。 */
    public static User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        String key = username.trim();
        User user = USERS.get(key);
        if (user != null && PASSWORDS.get(key).equals(password)) {
            return user;
        }
        return null;
    }

    public static User demoUser(Role role) {
        for (User user : USERS.values()) {
            if (user.getRole() == role) {
                return user;
            }
        }
        throw new IllegalStateException("没有该角色的演示账号");
    }
}
