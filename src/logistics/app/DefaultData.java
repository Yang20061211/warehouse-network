package logistics.app;

import java.util.List;
import logistics.model.City;

/**
 * 内置的 30 个城市数据。
 * 当 data/cities.txt 不存在或内容损坏时用它兜底，保证系统一定能启动。
 * 数据文件首次运行后会自动写回，用户之后可以在界面上修改。
 */
public final class DefaultData {

    private DefaultData() {
    }

    public static List<City> cities() {
        return List.of(
                new City("北京", 116.41, 39.90),
                new City("上海", 121.47, 31.23),
                new City("广州", 113.26, 23.13),
                new City("成都", 104.07, 30.67),
                new City("西安", 108.95, 34.27),
                new City("武汉", 114.31, 30.52),
                new City("杭州", 120.15, 30.28),
                new City("南京", 118.78, 32.06),
                new City("重庆", 106.55, 29.56),
                new City("天津", 117.20, 39.13),
                new City("沈阳", 123.43, 41.80),
                new City("哈尔滨", 126.53, 45.80),
                new City("长春", 125.32, 43.82),
                new City("济南", 117.00, 36.65),
                new City("青岛", 120.38, 36.07),
                new City("郑州", 113.63, 34.75),
                new City("长沙", 112.94, 28.23),
                new City("南昌", 115.89, 28.68),
                new City("福州", 119.30, 26.08),
                new City("厦门", 118.09, 24.48),
                new City("深圳", 114.06, 22.55),
                new City("南宁", 108.37, 22.82),
                new City("昆明", 102.83, 24.88),
                new City("贵阳", 106.63, 26.65),
                new City("兰州", 103.83, 36.06),
                new City("西宁", 101.78, 36.62),
                new City("银川", 106.23, 38.49),
                new City("乌鲁木齐", 87.62, 43.82),
                new City("拉萨", 91.11, 29.65),
                new City("呼和浩特", 111.75, 40.84)
        );
    }
}
