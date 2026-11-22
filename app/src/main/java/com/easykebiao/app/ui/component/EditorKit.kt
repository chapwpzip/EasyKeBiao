package com.easykebiao.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.easykebiao.app.R
import com.easykebiao.app.ui.theme.noRippleClickable
import com.nevoit.glasense.theme.GlasenseTheme

/**
 * 待办 / 备忘共用的「日程表单」小件。
 *
 * 版式参照常见的日程录入页：识别填写 → 标题 + 分类 → 截止与时间 → 地点 → 备注 → 优先级。
 * 这里只放纯展示与点击，状态留在各自弹窗里，避免两边各写一套样式又慢慢走形。
 */
object TodoCategory {
    const val TODO = "todo"
    const val ACTIVITY = "activity"
    const val EXAM = "exam"
    const val HOMEWORK = "homework"
    const val OTHER = "other"

    val keys = listOf(TODO, ACTIVITY, EXAM, HOMEWORK, OTHER)

    /** 颜色跟分类走：待办绿、活动橙、考试红、作业蓝、其他灰。 */
    fun color(key: String): Color = when (key) {
        TODO -> Color(0xFF34C759)
        ACTIVITY -> Color(0xFFFF9500)
        EXAM -> Color(0xFFFF3B30)
        HOMEWORK -> Color(0xFF007AFF)
        else -> Color(0xFF8E8E93)
    }

    @Composable
    fun label(key: String): String = stringResource(
        when (key) {
            TODO -> R.string.todo_cat_todo
            ACTIVITY -> R.string.todo_cat_activity
            EXAM -> R.string.todo_cat_exam
            HOMEWORK -> R.string.todo_cat_homework
            else -> R.string.todo_cat_other
        }
    )
}

/** 一档优先级：0 无 / 1 低 / 2 中 / 3 高。 */
@Composable
fun PriorityLabel(value: Int): String = stringResource(
    when (value) {
        1 -> R.string.todo_prio_low
        2 -> R.string.todo_prio_mid
        3 -> R.string.todo_prio_high
        else -> R.string.todo_prio_none
    }
)

/** 通用胶囊 chip：选中给底色 + 主色文字。 */
@Composable
fun EditorChip(
    label: String,
    selected: Boolean = false,
    tint: Color = GlasenseTheme.colors.primary,
    onClick: () -> Unit
) {
    val colors = GlasenseTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) tint.copy(alpha = 0.16f) else colors.contentVariant.copy(alpha = 0.10f))
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) tint else colors.contentVariant
        )
    }
}

/** 分类 chips：前面带一枚同色圆点，跟列表里的色标一致。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    value: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TodoCategory.keys.forEach { key ->
            val selected = key == value
            val tint = TodoCategory.color(key)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) tint.copy(alpha = 0.16f) else GlasenseTheme.colors.contentVariant.copy(alpha = 0.10f))
                    .noRippleClickable { onSelect(key) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(tint)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = TodoCategory.label(key),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) tint else GlasenseTheme.colors.contentVariant
                    )
                }
            }
        }
    }
}

/** 优先级 chips：无 / 低 / 中 / 高。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriorityChips(
    value: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..3).forEach { level ->
            val tint = when (level) {
                3 -> GlasenseTheme.colors.error
                2 -> Color(0xFFFF9500)
                1 -> Color(0xFF34C759)
                else -> GlasenseTheme.colors.primary
            }
            EditorChip(
                label = PriorityLabel(level),
                selected = level == value,
                tint = tint
            ) { onSelect(level) }
        }
    }
}

/**
 * 整页表单外壳（0.16.0）：日程式填写页字段多，AlertDialog 在横屏/小屏上会把下半截挤到
 * 滚动区外。这里用接近全屏的卡片 + 固定底部动作条，字段区自己滚动。
 */
@Composable
fun EditorSheet(
    onDismiss: () -> Unit,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
    actions: @Composable () -> Unit
) {
    val colors = GlasenseTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(26.dp),
            color = colors.cardBackground
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) { header() }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = null, tint = colors.contentVariant)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.weight(1f)) { content() }
                Spacer(modifier = Modifier.height(10.dp))
                actions()
            }
        }
    }
}

