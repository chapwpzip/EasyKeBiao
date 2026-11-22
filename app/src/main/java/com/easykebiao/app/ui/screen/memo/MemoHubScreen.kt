package com.easykebiao.app.ui.screen.memo

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easykebiao.app.R
import com.easykebiao.app.ui.component.applePressScale
import com.easykebiao.app.ui.screen.todo.TodoPane
import com.easykebiao.app.ui.theme.AppLayout
import com.easykebiao.app.ui.theme.noRippleClickable
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme

/**
 * 备忘录栏目 = 待办 + 备忘。
 *
 * 为什么放在同一栏：两者是同一件事的两种形态（"要做" 和 "记下"），
 * 分两个 Dock tab 会把底栏挤到 6 格（320dp 窄屏会顶格），所以顶部用分段切换。
 */
@Composable
fun MemoHubScreen() {
    val g = GlasenseTheme.colors
    var segment by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize().background(g.pageBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.widthIn(max = AppLayout.readingWidth).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.memo_title),
                    style = GlasenseTheme.type.largeTitleEmphasized,
                    color = g.content
                )
                VGap(4.dp)
                Text(
                    text = stringResource(R.string.memo_hub_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = g.contentVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                SegmentBar(
                    labels = listOf(
                        stringResource(R.string.todo_segment),
                        stringResource(R.string.memo_segment)
                    ),
                    selected = segment,
                    onSelect = { segment = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            when (segment) {
                0 -> TodoPane(modifier = Modifier.weight(1f))
                else -> MemoPane(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** iOS 分段控件：胶囊底 + 滑块，切换时颜色渐变。 */
@Composable
private fun SegmentBar(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val g = GlasenseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(g.contentVariant.copy(alpha = 0.10f))
            .padding(3.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            val bg by animateColorAsState(
                targetValue = if (active) g.cardBackground else g.cardBackground.copy(alpha = 0f),
                label = "segment-bg"
            )
            val fg by animateColorAsState(
                targetValue = if (active) g.content else g.contentVariant,
                label = "segment-fg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .applePressScale(0.97f)
                    .noRippleClickable { onSelect(index) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = fg
                )
            }
        }
    }
}
