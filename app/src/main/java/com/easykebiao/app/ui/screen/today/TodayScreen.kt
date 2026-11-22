package com.easykebiao.app.ui.screen.today

import androidx.compose.ui.draw.alpha
import com.easykebiao.app.ui.component.rememberCourseClock
import com.easykebiao.app.util.CourseCompletion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.saveable.rememberSaveable
import com.easykebiao.app.ui.theme.AppLayout
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.easykebiao.app.R
import com.easykebiao.app.data.entity.CourseEntity
import com.easykebiao.app.ui.component.CourseDetailSheet
import com.easykebiao.app.ui.component.LocalNavExtraBottomPadding
import com.easykebiao.app.ui.screen.schedule.ScheduleViewModel
import com.easykebiao.app.ui.theme.noRippleClickable
import com.easykebiao.app.util.AppPrefs
import com.easykebiao.app.util.CourseColorUtil
import com.easykebiao.app.util.DateUtils
import com.easykebiao.app.util.TimeTableUtils
import com.nevoit.glasense.core.component.HGap
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import java.time.LocalDate

/**
 * 今日页 —— Glasense 视觉层（UI-1，2026-09-25）。
 *
 * 数据来源、周次/调休/学期判定与冲突引擎保持一致。
 * P1：阅读限宽；冲突栏按可用宽度/字体换行；详情选择保存 ID。
 *   · 页面底色走 Glasense pageBackground（中性）
 *   · 头部由「大圆角卡片」改为 Glasense 的「大标题 + 日期 + 状态胶囊」
 *   · 课程卡由「高饱和满铺色块」改为「中性卡 + 课程色色条 + 中性时间字」，
 *     信息层级靠字号与颜色深浅区分（不再靠大色块压过内容）
 *   · 圆角统一由 Glasense specs 提供（卡片 12dp）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    onEditCourse: (CourseEntity) -> Unit = {},
    viewModel: ScheduleViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val now = rememberCourseClock()
    val today = now.toLocalDate()
    val actualWeek = state.currentTable?.let { DateUtils.currentWeek(it.startDate, today) } ?: state.currentWeek
    // 学期外感知: BEFORE_START/AFTER_END 时今日课不按周过滤展示
    val semesterStatus = state.currentTable?.let {
        val source = com.easykebiao.app.util.CourseDateResolver.teachingDate(today, state.transfers) ?: today
        DateUtils.semesterStatus(it.startDate, it.maxWeek, source)
    } ?: DateUtils.SemesterStatus.IN_RANGE
    val todayCourses = state.effectiveCurrentTable?.let { table ->
        com.easykebiao.app.util.CourseDateResolver.coursesOn(today, table.startDate, table.maxWeek, state.courses, state.transfers)
    }.orEmpty().let { list ->
        // 用户报障 2026-09-10: ownTime 课渲染前按真实时间归一化节点(与网格同一预处理),
        // 落库的表单占位节点不再影响今日页分组与显示。
        val tj = state.effectiveCurrentTable?.timeJson
        if (tj == null) list else list.map { c -> c.normalizeNode(tj) }
    }.sortedBy { it.startNode }

    var selectedCourseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedCourse = todayCourses.firstOrNull { it.id == selectedCourseId }

    // 天气：内存快照即时可用；首次进入时拉一次（失败静默，卡片上就不显示天气）
    val context = LocalContext.current
    val weatherEnabled = AppPrefs.isWeatherEnabled(context)
    var weather by remember { mutableStateOf(com.easykebiao.app.data.weather.WeatherRepository.peek()) }
    androidx.compose.runtime.LaunchedEffect(weatherEnabled) {
        if (weatherEnabled) {
            weather = com.easykebiao.app.data.weather.WeatherRepository.refreshIfStale(context)
        }
    }

    TodayContent(today, actualWeek, semesterStatus, todayCourses,
        state.effectiveCurrentTable?.timeJson, now, holidayGrey = state.holidaySnapshot.grey(today, state.transfers),
        weather = if (weatherEnabled) weather else null,
        onSelectCourse = { selectedCourseId = it.id })

    // 详情 Bottom Sheet — 与课表页同一组件同一交互（视觉换装留待 UI-3）
    CourseDetailSheet(
        course = selectedCourse,
        timeString = selectedCourse?.let { it.nodeString(LocalContext.current) },
        allCourses = todayCourses,
        timeJson = state.effectiveCurrentTable?.timeJson,
        onDismiss = { selectedCourseId = null },
        onEdit = { course ->
            selectedCourseId = null
            onEditCourse(course)
        }
    )
}

/** Stateless sample surface; production and debug fixtures use the same layout. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TodayContent(
    today: LocalDate,
    actualWeek: Int,
    semesterStatus: DateUtils.SemesterStatus,
    todayCourses: List<CourseEntity>,
    timeJson: String?,
    now: java.time.LocalDateTime,
    holidayGrey: Boolean = false,
    weather: com.easykebiao.app.data.weather.WeatherSnapshot? = null,
    onSelectCourse: (CourseEntity) -> Unit
) {
    // v7.10.10 今日页冲突分栏 — 与周视图同一引擎同一分组(weekLaneRows):
    // 同一冲突区域保持一组，宽度不足时各栏纵向排列，不改变引擎分组。
    // 分组在 LazyColumn 外 remember(LazyListScope 非 composable 上下文)。
    val laneRows = remember(todayCourses, timeJson) {
        com.easykebiao.app.util.ConflictLayoutEngine.weekLaneRows(
            todayCourses, timeJson
        )
    }

    // Dock 悬浮底栏: 滚动尾部多留 Dock 总高, 最后一项能滚到 Dock 上方(FAB 语义)
    val navExtra = LocalNavExtraBottomPadding.current
    val g = GlasenseTheme.colors
    Box(Modifier.fillMaxSize().background(g.pageBackground), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = AppLayout.readingWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                // UI-25a：20dp → 16dp（同"我的"页：全 App 统一 16dp 左线）
                start = AppLayout.pagePadding, end = AppLayout.pagePadding, top = 4.dp, bottom = AppLayout.pagePadding + navExtra
            ),
            verticalArrangement = Arrangement.spacedBy(AppLayout.itemSpacing)
        ) {
            item {
                TodayHeroHeader(
                    date = today, week = actualWeek,
                    count = todayCourses.size, semesterStatus = semesterStatus
                )
            }

            if (todayCourses.isEmpty()) {
                item { EmptyToday(semesterStatus = semesterStatus) }
            } else {
                // v7.10.10 今日页冲突分栏 — 分组已提至 LazyColumn 外
                laneRows.forEach { row ->
                    if (row.laneCount == 1) {
                        item(key = row.courses[0].id) {
                            TodayCourseCard(
                                course = row.courses[0],
                                timeJson = timeJson,
                                now = now,
                                holidayGrey = holidayGrey,
                                today = today,
                                weather = weather,
                                onClick = { onSelectCourse(row.courses[0]) },
                                groupRows = todayCourses.filter { it.groupId == row.courses[0].groupId }
                            )
                        }
                    } else {
                        item(key = "conflict-${row.courses.first().id}") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.import_conflicts),
                                    style = GlasenseTheme.type.footnoteEmphasized,
                                    color = g.contentVariant
                                )
                                val laneGap = 10.dp
                                BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    val laneWidth = (maxWidth - laneGap * (row.laneCount - 1)) / row.laneCount
                                    val stackLanes = laneWidth < 156.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(laneGap),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        repeat(row.laneCount) { li ->
                                            val laneCourses = row.courses.filter { row.laneOf[it.id] == li }
                                            Column(
                                                modifier = if (stackLanes) Modifier.fillMaxWidth() else Modifier.width(laneWidth),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                laneCourses.forEach { laneCourse ->
                                                    TodayCourseCard(
                                                        course = laneCourse,
                                                        timeJson = timeJson,
                                                        now = now,
                                holidayGrey = holidayGrey,
                                                        today = today,
                                                        weather = weather,
                                                        onClick = { onSelectCourse(laneCourse) },
                                                        groupRows = todayCourses.filter { it.groupId == laneCourse.groupId }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

}

/** 大标题 + 日期 + 状态胶囊 —— Glasense 的「大标题头」版式（替代原大圆角卡片）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayHeroHeader(
    date: LocalDate,
    week: Int,
    count: Int,
    semesterStatus: DateUtils.SemesterStatus = DateUtils.SemesterStatus.IN_RANGE
) {
    val g = GlasenseTheme.colors
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = stringResource(R.string.today_today),
            style = GlasenseTheme.type.footnoteEmphasized,
            color = g.contentVariant
        )
        VGap(4.dp)
        /** 标题行：日期用 largeTitleEmphasized(32sp 半粗)，星期紧随其后、基线略上抬 */
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.date_long_format, date.monthValue, date.dayOfMonth),
                style = GlasenseTheme.type.largeTitleEmphasized,
                color = g.content
            )
            Text(
                text = DateUtils.localizedDay(date.dayOfWeek.value, context),
                style = GlasenseTheme.type.title3Emphasized,
                color = g.contentVariant,
                modifier = Modifier.align(Alignment.Bottom).padding(bottom = 4.dp)
            )
        }
        VGap(10.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 学期外: 周次 chip 换学期状态, 不再显示误导性的"第 1 周"
            when (semesterStatus) {
                DateUtils.SemesterStatus.BEFORE_START ->
                    StatPill(stringResource(R.string.semester_not_started))
                DateUtils.SemesterStatus.AFTER_END ->
                    StatPill(stringResource(R.string.semester_ended))
                else ->
                    StatPill(stringResource(R.string.schedule_current_week, week))
            }
            StatPill(
                if (count == 0) stringResource(R.string.no_course)
                else stringResource(R.string.n_course_periods, count)
            )
        }
    }
}

/** 状态胶囊 —— 卡面底 + 次要文字色（页面是 #F3F4F6 浅灰，胶囊用卡面白才分得清）。
 *  Glasense 的 scrim 系列只有 5%~20% 黑，直接铺在页面底上几乎看不见，故取 cardBackground。 */
@Composable
private fun StatPill(label: String) {
    val g = GlasenseTheme.colors
    Text(
        text = label,
        style = GlasenseTheme.type.footnoteEmphasized,
        color = g.contentVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(g.cardBackground)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun EmptyToday(semesterStatus: DateUtils.SemesterStatus = DateUtils.SemesterStatus.IN_RANGE) {
    val g = GlasenseTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlasenseTheme.specs.cardShape)
            .background(g.cardBackground)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = g.contentVariant,
            modifier = Modifier.size(40.dp)
        )
        val (title, hint) = when (semesterStatus) {
            DateUtils.SemesterStatus.BEFORE_START ->
                stringResource(R.string.semester_not_started) to stringResource(R.string.today_semester_out_hint)
            DateUtils.SemesterStatus.AFTER_END ->
                stringResource(R.string.semester_ended) to stringResource(R.string.today_semester_out_hint)
            else ->
                stringResource(R.string.schedule_no_course_today) to stringResource(R.string.today_no_course)
        }
        Text(text = title, style = GlasenseTheme.type.subHeadlineEmphasized, color = g.content)
        Text(text = hint, style = GlasenseTheme.type.footnote, color = g.contentVariant)
    }
}

/**
 * 课程卡 —— 中性卡面 + 课程色色条 + 中性时间字。
 * 原实现是「课程色满铺卡面 + 自动反色文字」，饱和度高、整屏色块抢眼；
 * 这里把课程色降级为**强调用色**（柔和色条），文字回到中性前景，阅读层级更清楚。
 * 冲突分栏（同 groupId 多行取色）与无色模式逻辑保持不变。
 */
@Composable
private fun TodayCourseCard(
    course: CourseEntity,
    now: java.time.LocalDateTime,
    holidayGrey: Boolean = false,
    timeJson: String? = null,
    today: LocalDate = now.toLocalDate(),
    weather: com.easykebiao.app.data.weather.WeatherSnapshot? = null,
    onClick: (() -> Unit)? = null,
    groupRows: List<CourseEntity> = listOf(course)
) {
    val g = GlasenseTheme.colors
    val context = LocalContext.current
    val colorless = AppPrefs.isCourseColorless(context)
    // 统一取色入口 — 种子是**课程名**（同门课在任何课表/任何一次导入里恒为同色）。
    // 以稳定课程色为源，绘制时统一柔化；时间文字独立使用主题前景色。
    // issue#22: 同名课程多地点 — 用 groupRows 传同 groupId 全行, 支持 AUTO/CUSTOM 模式取色
    val accent = if (colorless) g.contentVariant else CourseColorUtil.pickAccentComposeWithGroupRows(
        row = course,
        groupRows = groupRows,
        isDark = GlasenseTheme.darkTheme,
        neutralColor = g.contentVariant,
        colorless = false,
        slot = com.easykebiao.app.util.LocalCourseColorSlots.current[CourseColorUtil.colorSeed(course)]
    )
    val time = if (course.ownTime && course.startTime.isNotBlank() && course.endTime.isNotBlank()) {
        "${course.startTime}-${course.endTime}"
    } else {
        timeJson?.let { TimeTableUtils.courseTimeString(course.startNode, course.step, it) }
    }

    Row(
        modifier = Modifier
            .alpha(com.easykebiao.app.util.CourseDimming.opacity(CourseCompletion.isCompleted(course, now.toLocalDate(), timeJson, now), holidayGrey))
            .fillMaxWidth()
            .heightIn(min = AppLayout.minimumTouchTarget)
            .height(IntrinsicSize.Min)
            .clip(GlasenseTheme.specs.cardShape)
            .background(g.cardBackground)
            .then(if (onClick != null) Modifier.noRippleClickable(onClick = onClick) else Modifier)
            .padding(start = 10.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 课程色色条（贴在卡片左缘，高度随内容）
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(CourseColorUtil.softenedIndicator(accent, g.cardBackground))
        )
        // 天气（图标 + 温度）—— 2026-11-22: 用户要求「和课程一块显示，放在课程左边」。
        // 只在该课开始那一小时的天气拿得到时才画；拿不到就整块不占位（不是显示 "--"）。
        val hour = remember(course.id, today, timeJson, weather) {
            val start = courseStartTime(course, timeJson)
            if (start == null || weather == null) null else weather.courseHour(today, start)
        }
        if (hour != null) {
            HGap(8.dp)
            WeatherBadge(hour)
            HGap(10.dp)
        } else {
            HGap(12.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.courseName,
                style = GlasenseTheme.type.headline,
                color = g.content,
            )
            VGap(4.dp)
            Text(
                text = listOfNotNull(course.shortNodeString(context), time).joinToString(" · "),
                style = GlasenseTheme.type.footnote,
                color = g.contentVariant
            )
            if (course.teacher.isNotBlank() || course.room.isNotBlank()) {
                VGap(4.dp)
                val meta = buildString {
                    if (course.teacher.isNotBlank()) append(course.teacher)
                    if (course.room.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(course.room)
                    }
                }
                Text(
                    text = meta,
                    style = GlasenseTheme.type.footnote,
                    color = g.contentVariant,
                )
            }
        }
    }
}

// ===== 天气（0.14.0，2026-11-22）=====

/**
 * 这门课「开始那一刻」的时刻。
 * 优先用授课表作息算（startNode → 起始时间），ownTime 课直接读自己写的 startTime。
 * 都算不出来就返回 null —— 调用方据此不显示天气，而不是猜一个整点。
 */
private fun courseStartTime(course: CourseEntity, timeJson: String?): java.time.LocalTime? {
    val raw = timeJson?.let {
        com.easykebiao.app.util.TimeTableUtils.courseTimeParts(
            courseStartNode = course.startNode,
            courseStep = course.step,
            timeJson = it,
            ownTime = course.ownTime,
            startTime = course.startTime,
            endTime = course.endTime
        )?.first
    }?.trim()?.takeIf { it.isNotEmpty() }
        ?: course.startTime.trim().takeIf { it.isNotEmpty() }
    return raw?.let { runCatching { java.time.LocalTime.parse(it) }.getOrNull() }
}

/** 课程卡左侧的天气徽标：上图标、下温度（只在这里出现，不额外占一行）。 */
@Composable
private fun WeatherBadge(hour: com.easykebiao.app.data.weather.WeatherHour) {
    val g = GlasenseTheme.colors
    val tint = if (com.easykebiao.app.data.weather.WeatherCode.isWet(hour.code)) {
        // 有雨雪：用课程页里最接近"提示"的色，但仍是中性前景，避免整屏花花绿绿
        g.contentVariant
    } else {
        g.contentVariant
    }
    Column(
        modifier = Modifier.width(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Icon(
            imageVector = weatherIcon(hour.kind),
            contentDescription = com.easykebiao.app.data.weather.WeatherCode.shortLabelZh(hour.code),
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "${kotlin.math.round(hour.celsius).toInt()}°",
            style = GlasenseTheme.type.footnote,
            color = g.contentVariant,
            maxLines = 1
        )
    }
}

/** WMO 大类 → 线性图标（material-icons-extended 里现成的，不引第三方图标包）。 */
private fun weatherIcon(kind: com.easykebiao.app.data.weather.WeatherKind): androidx.compose.ui.graphics.vector.ImageVector =
    when (kind) {
        com.easykebiao.app.data.weather.WeatherKind.SUN -> Icons.Outlined.WbSunny
        com.easykebiao.app.data.weather.WeatherKind.PARTLY -> Icons.Outlined.WbCloudy
        com.easykebiao.app.data.weather.WeatherKind.CLOUD -> Icons.Outlined.Cloud
        com.easykebiao.app.data.weather.WeatherKind.FOG -> Icons.Outlined.BlurOn
        com.easykebiao.app.data.weather.WeatherKind.DRIZZLE -> Icons.Outlined.Grain
        com.easykebiao.app.data.weather.WeatherKind.RAIN -> Icons.Outlined.Umbrella
        com.easykebiao.app.data.weather.WeatherKind.SNOW -> Icons.Outlined.AcUnit
        com.easykebiao.app.data.weather.WeatherKind.THUNDER -> Icons.Outlined.Thunderstorm
    }
