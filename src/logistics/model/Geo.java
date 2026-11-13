package logistics.model;

/**
 * 经纬度距离工具。
 *
 * <p>作业要求：任意两城之间的边权 = 按经纬度计算的欧氏距离，保留 2 位小数。
 * 这里严格按题目定义，直接把经纬度当作平面坐标计算：
 * {@code sqrt((lon1-lon2)^2 + (lat1-lat2)^2)}。
 * 单位是“度”，1 度约等于 111 公里，可在界面上按需换算展示。</p>
 */
public final class Geo {

    /** 1 个纬度大约对应的公里数，仅用于展示换算。 */
    public static final double KM_PER_DEGREE = 111.0;

    private Geo() {
    }

    /** 原始欧氏距离（未取整）。 */
    public static double euclid(City a, City b) {
        double dx = a.getLon() - b.getLon();
        double dy = a.getLat() - b.getLat();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** 边权：欧氏距离保留 2 位小数。 */
    public static double weight(City a, City b) {
        return round2(euclid(a, b));
    }

    /** 四舍五入到 2 位小数。 */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
