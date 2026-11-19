package com.imsx3d.classy.ui.screen.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imsx3d.classy.R
import com.imsx3d.classy.data.entity.MemoEntity
import com.imsx3d.classy.ui.component.DialogActionButtons
import com.imsx3d.classy.ui.component.GlasenseIconButton
import com.imsx3d.classy.ui.component.LocalNavExtraBottomPadding
import com.imsx3d.classy.ui.component.SettingsScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import com.imsx3d.classy.ui.component.appGlassTint
import com.imsx3d.classy.ui.component.applePressScale
import com.imsx3d.classy.ui.component.settingsCard
import com.imsx3d.classy.ui.theme.ClassyLayout
import com.imsx3d.classy.ui.theme.SleepyTheme
import com.imsx3d.classy.ui.theme.noRippleClickable
import com.imsx3d.classy.ui.glass.GlasenseButton
import com.imsx3d.classy.ui.glass.GlasenseButtonColors
import com.imsx3d.classy.ui.glass.shaderRipple
import com.kyant.shapes.Capsule
import androidx.compose.foundation.isSystemInDarkTheme
import com.imsx3d.classy.ui.theme.primaryFilledButtonColors
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 备忘录 —— 底部功能区（Dock）的一个 tab。
 *
 * 与课表数据完全解耦（独立的 memos 表），编辑走弹窗而不是二级页面：
 * 备忘录的编辑面只有标题 + 正文两栏，为一个弹窗再开一个路由只会多一次转场。
 * 列表顺序由 DAO 给定（置顶优先，其次按最近修改）。
 */
@Composable
fun MemoPane(modifier: Modifier = Modifier, viewModel: MemoViewModel = viewModel()) {
    val memos by viewModel.memos.collectAsState()
    val untitled = stringResource(R.string.memo_untitled)
    val g = GlasenseTheme.colors
    val dialog = remember { MemoDialogState() }
    // Dock 悬浮底栏：滚动尾部多留 Dock 总高（与「我的」「今日」同一处理）
    val navExtra = LocalNavExtraBottomPadding.current

    Box(
        modifier = modifier.fillMaxSize().background(g.pageBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = ClassyLayout.readingWidth).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp + navExtra),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            memoEntries(
                memos = memos,
                untitled = untitled,
                onEdit = { dialog.editing = it },
                onPin = { viewModel.setPinned(it, !it.pinned) },
                onDelete = { dialog.deleteTarget = it }
            )
        }
        // 功能键：Glasense 按钮（按下有弹簧缩放 + 白色高光叠加），浮在列表底部
        GlasenseButton(
            onClick = { dialog.creating = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = ClassyLayout.readingWidth)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 10.dp + navExtra
                ),
            shape = Capsule(),
            colors = GlasenseButtonColors(
                containerColor = g.primary,
                contentColor = Color.White,
                disabledContainerColor = g.primary.copy(alpha = 0.45f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.memo_new), color = Color.White)
            }
        }
    }

    MemoDialogs(dialog = dialog, viewModel = viewModel)
}

/**
 * 路由版（`SleepyRoute.Memo`）—— 供深链 / 旧入口使用，带返回箭头。
 * 与 tab 版共用同一份列表与弹窗实现，避免两处逻辑漂移。
 */
@Composable
fun MemoScreen(
    onBack: () -> Unit,
    viewModel: MemoViewModel = viewModel()
) {
    val memos by viewModel.memos.collectAsState()
    val untitled = stringResource(R.string.memo_untitled)
    val dialog = remember { MemoDialogState() }

    SettingsScaffold(
        title = stringResource(R.string.memo_title),
        onBack = onBack,
        verticalSpacing = 10.dp,
        bottomCTA = {
            GlasenseButton(
                onClick = { dialog.creating = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = Capsule(),
                colors = GlasenseButtonColors(
                    containerColor = GlasenseTheme.colors.primary,
                    contentColor = Color.White,
                    disabledContainerColor = GlasenseTheme.colors.primary.copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.memo_new), color = Color.White)
                }
            }
        }
    ) {
        memoEntries(
            memos = memos,
            untitled = untitled,
            onEdit = { dialog.editing = it },
            onPin = { viewModel.setPinned(it, !it.pinned) },
            onDelete = { dialog.deleteTarget = it }
        )
    }

    MemoDialogs(dialog = dialog, viewModel = viewModel)
}

/** 列表条目（空态 + 每一条）—— tab 版与路由版只在这里画一次。 */
private fun LazyListScope.memoEntries(
    memos: List<MemoEntity>,
    untitled: String,
    onEdit: (MemoEntity) -> Unit,
    onPin: (MemoEntity) -> Unit,
    onDelete: (MemoEntity) -> Unit
) {
    if (memos.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.memo_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = GlasenseTheme.colors.contentVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            )
        }
    }
    itemsIndexed(memos, key = { _, memo -> memo.id }) { index, memo ->
        // 新建/删除/置顶换位都走弹簧位移，写下去的东西"落"到该在的位置
        MemoRow(
            memo = memo,
            untitled = untitled,
            onEdit = onEdit,
            onPin = onPin,
            onDelete = onDelete,
            // iOS inset-grouped：整组只有最外圈是圆角，行间用发丝线分隔
            groupedShape = memoGroupShape(first = index == 0, last = index == memos.lastIndex),
            showDivider = index != memos.lastIndex,
            modifier = Modifier.animateItem()
        )
    }
}

/** 分组卡的外圈圆角 —— 组内首行圆上沿、末行圆下沿，中间行直角。 */
private fun memoGroupShape(first: Boolean, last: Boolean): Shape {
    val r = 12.dp
    return RoundedCornerShape(
        topStart = if (first) r else 0.dp,
        topEnd = if (first) r else 0.dp,
        bottomStart = if (last) r else 0.dp,
        bottomEnd = if (last) r else 0.dp
    )
}

/** 单条备忘录：标题（置顶带图钉）+ 正文两行 + 时间戳 + 行内置顶/删除。 */
@Composable
private fun MemoRow(
    memo: MemoEntity,
    untitled: String,
    onEdit: (MemoEntity) -> Unit,
    onPin: (MemoEntity) -> Unit,
    onDelete: (MemoEntity) -> Unit,
    groupedShape: Shape,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().clip(groupedShape).background(GlasenseTheme.colors.cardBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .applePressScale()
                .shaderRipple(dark = isSystemInDarkTheme())
                .noRippleClickable { onEdit(memo) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (memo.pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = memo.title.ifBlank { untitled },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (memo.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memo.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = memoTimeText(memo.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
        GlasenseIconButton(
            icon = Icons.Outlined.PushPin,
            contentDescription = stringResource(R.string.memo_pin),
            onClick = { onPin(memo) },
            tint = if (memo.pinned) colors.primary else colors.onSurfaceVariant
        )
        GlasenseIconButton(
            icon = Icons.Outlined.Delete,
            contentDescription = stringResource(R.string.memo_delete),
            onClick = { onDelete(memo) }
        )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(1.dp)
                    .background(colors.outlineVariant.copy(alpha = 0.6f))
            )
        }
    }
}

/** 弹窗状态：creating = 新建；editing = 编辑既有条目（二者互斥）；deleteTarget = 二次确认中。 */
private class MemoDialogState {
    var creating by mutableStateOf(false)
    var editing by mutableStateOf<MemoEntity?>(null)
    var deleteTarget by mutableStateOf<MemoEntity?>(null)
}

@Composable
private fun MemoDialogs(dialog: MemoDialogState, viewModel: MemoViewModel) {
    val colors = MaterialTheme.colorScheme

    if (dialog.creating || dialog.editing != null) {
        MemoEditorDialog(
            memo = dialog.editing,
            onDismiss = { dialog.creating = false; dialog.editing = null },
            onSave = { title, content, pinned ->
                viewModel.save(dialog.editing?.id, title, content, pinned)
                dialog.creating = false
                dialog.editing = null
            }
        )
    }

    dialog.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { dialog.deleteTarget = null },
            title = { Text(stringResource(R.string.memo_delete), color = colors.onSurface) },
            confirmButton = {},
            dismissButton = {},
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.memo_delete_confirm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DialogActionButtons(
                        confirmText = stringResource(R.string.memo_delete),
                        onConfirm = {
                            viewModel.delete(target)
                            dialog.deleteTarget = null
                        },
                        dismissText = stringResource(R.string.cancel),
                        onDismiss = { dialog.deleteTarget = null },
                        destructive = true
                    )
                }
            }
        )
    }
}

/** 标题 + 正文 + 置顶开关 —— 备忘录的全部字段，别的一概不放。 */
@Composable
private fun MemoEditorDialog(
    memo: MemoEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, pinned: Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var title by remember(memo?.id) { mutableStateOf(memo?.title.orEmpty()) }
    var content by remember(memo?.id) { mutableStateOf(memo?.content.orEmpty()) }
    var pinned by remember(memo?.id) { mutableStateOf(memo?.pinned ?: false) }
    val canSave = title.isNotBlank() || content.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(if (memo == null) R.string.memo_new else R.string.memo_edit),
                color = colors.onSurface
            )
        },
        confirmButton = {},
        dismissButton = {},
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.memo_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors()
                )
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.memo_content_label)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleClickable { pinned = !pinned }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.memo_pin),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
                DialogActionButtons(
                    confirmText = stringResource(R.string.ok),
                    onConfirm = { if (canSave) onSave(title.trim(), content.trim(), pinned) },
                    dismissText = stringResource(R.string.cancel),
                    onDismiss = onDismiss,
                    confirmEnabled = canSave
                )
            }
        }
    )
}

/** 列表里的时间戳：同一年的只给「月-日 时:分」，跨年才带年份，避免每行都写满一串。 */
private fun memoTimeText(millis: Long): String {
    val nowYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    val memoYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(millis))
    val pattern = if (nowYear == memoYear) "MM-dd HH:mm" else "yyyy-MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}
