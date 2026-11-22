package com.easykebiao.app.data.weather

/**
 * 内置城市表 —— 手选城市用，不申请定位权限。
 *
 * 只收录常见城市（教学场景够用）；用户所在城市不在表里时，
 * 设置页会提示"暂未收录"，而不是偷偷用定位或猜一个坐标。
 */
data class WeatherCity(val name: String, val lat: Double, val lon: Double) {
    /** 城市键（用于比较"缓存是不是这个城市的"）。 */
    val key: String get() = name
}

object CityCatalog {

    /** 默认城市（桂林旅游学院所在城市）—— 与演示数据同源，装完就能看到效果。 */
    val DEFAULT = WeatherCity("桂林", 25.2736, 110.2900)

    val all: List<WeatherCity> = listOf(
        WeatherCity("北京", 39.9042, 116.4074),
        WeatherCity("上海", 31.2304, 121.4737),
        WeatherCity("天津", 39.3434, 117.3616),
        WeatherCity("重庆", 29.5630, 106.5516),
        WeatherCity("广州", 23.1291, 113.2644),
        WeatherCity("深圳", 22.5431, 114.0579),
        WeatherCity("杭州", 30.2741, 120.1551),
        WeatherCity("南京", 32.0603, 118.7969),
        WeatherCity("武汉", 30.5928, 114.3055),
        WeatherCity("成都", 30.5728, 104.0668),
        WeatherCity("西安", 34.3416, 108.9398),
        WeatherCity("苏州", 31.2989, 120.5853),
        WeatherCity("长沙", 28.2282, 112.9388),
        WeatherCity("郑州", 34.7466, 113.6254),
        WeatherCity("青岛", 36.0671, 120.3826),
        WeatherCity("济南", 36.6512, 117.1201),
        WeatherCity("合肥", 31.8206, 117.2272),
        WeatherCity("福州", 26.0745, 119.2965),
        WeatherCity("厦门", 24.4798, 118.0894),
        WeatherCity("南昌", 28.6820, 115.8579),
        WeatherCity("昆明", 24.8801, 102.8329),
        WeatherCity("贵阳", 26.6470, 106.6302),
        WeatherCity("南宁", 22.8170, 108.3665),
        WeatherCity("桂林", 25.2736, 110.2900),
        WeatherCity("海口", 20.0444, 110.1999),
        WeatherCity("太原", 37.8706, 112.5489),
        WeatherCity("石家庄", 38.0428, 114.5149),
        WeatherCity("沈阳", 41.8057, 123.4315),
        WeatherCity("大连", 38.9140, 121.6147),
        WeatherCity("长春", 43.8171, 125.3235),
        WeatherCity("哈尔滨", 45.8038, 126.5350),
        WeatherCity("呼和浩特", 40.8414, 111.7519),
        WeatherCity("银川", 38.4872, 106.2309),
        WeatherCity("兰州", 36.0611, 103.8343),
        WeatherCity("西宁", 36.6171, 101.7782),
        WeatherCity("乌鲁木齐", 43.8256, 87.6168),
        WeatherCity("拉萨", 29.6520, 91.1721),
        WeatherCity("香港", 22.3193, 114.1694),
        WeatherCity("澳门", 22.1987, 113.5439),
        WeatherCity("台北", 25.0330, 121.5654)
    )

    fun find(name: String): WeatherCity? = all.firstOrNull { it.name == name }

    /** 按名字模糊匹配（用户输入"桂"也能找到"桂林"）。 */
    fun search(query: String): List<WeatherCity> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter { it.name.contains(q) }
    }
}
