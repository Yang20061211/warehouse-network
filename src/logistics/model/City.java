package logistics.model;

import java.util.Objects;

/**
 * 城市结点：名称 + 经度 + 纬度。
 *
 * <p>对象设计成不可变的：修改坐标时生成新对象，
 * 避免链表结点与外层引用看到不一致的数据。</p>
 */
public final class City {

    private final String name;
    private final double lon;
    private final double lat;

    public City(String name, double lon, double lat) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("城市名称不能为空");
        }
        this.name = name.trim();
        this.lon = lon;
        this.lat = lat;
    }

    public String getName() {
        return name;
    }

    /** 经度。 */
    public double getLon() {
        return lon;
    }

    /** 纬度。 */
    public double getLat() {
        return lat;
    }

    public City withPosition(double newLon, double newLat) {
        return new City(name, newLon, newLat);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof City other)) {
            return false;
        }
        return name.equals(other.name)
                && Double.compare(lon, other.lon) == 0
                && Double.compare(lat, other.lat) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, lon, lat);
    }
}
