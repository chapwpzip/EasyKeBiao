package com.easykebiao.app.ui.screen.memo

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.easykebiao.app.data.ai.AiMemoClient
import com.easykebiao.app.ui.component.CategoryChips
import com.easykebiao.app.ui.component.PriorityChips
import com.easykebiao.app.ui.component.EditorChip
import com.easykebiao.app.ui.component.EditorSheet
import androidx.compose.material.icons.outlined.Place
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.easykebiao.app.R
import com.easykebiao.app.data.entity.MemoEntity
import com.easykebiao.app.data.memo.MemoImages
import com.easykebiao.app.ui.component.MemoThumb
import com.easykebiao.app.ui.component.MemoThumbRow
import com.easykebiao.app.ui.component.AiOrganizeDialog
import com.easykebiao.app.ui.component.DialogActionButtons
import com.easykebiao.app.ui.component.GlasenseIconButton
import com.easykebiao.app.ui.component.LocalNavExtraBottomPadding
import com.easykebiao.app.ui.component.SettingsScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import com.easykebiao.app.ui.component.appGlassTint
import com.easykebiao.app.ui.component.applePressScale
import com.easykebiao.app.ui.component.settingsCard
import com.easykebiao.app.ui.theme.AppLayout
import com.easykebiao.app.ui.theme.SleepyTheme
import com.easykebiao.app.ui.theme.noRippleClickable
import com.easykebiao.app.ui.glass.GlasenseButton
import com.easykebiao.app.ui.glass.GlasenseButtonColors
import com.easykebiao.app.ui.glass.shaderRipple
import com.kyant.shapes.Capsule
import androidx.compose.foundation.isSystemInDarkTheme
import com.easykebiao.app.ui.theme.primaryFilledButtonColors
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
            modifier = Modifier.widthIn(max = AppLayout.readingWidth).fillMaxSize(),
            // 底部要给悬浮 CTA 让位：CTA 高 56dp + 上下各 10dp，再叠 Dock 实测高度
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 88.dp + navExtra),
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
                .widthIn(max = AppLayout.readingWidth)
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
            val images = remember(memo.content) { MemoImages.imagesOf(memo.content) }
            val previewText = remember(memo.content) { MemoImages.strip(memo.content) }
            if (images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                MemoThumbRow(names = images, size = 56.dp, limit = 3)
            }
            if (previewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = previewText,
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
    val ctx = LocalContext.current

    if (dialog.creating || dialog.editing != null) {
        MemoEditorDialog(
            memo = dialog.editing,
            onDismiss = { dialog.creating = false; dialog.editing = null },
            onSave = { title, content, pinned, category, priority, place ->
                viewModel.save(dialog.editing?.id, title, content, pinned, category, priority, place)
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
                            MemoImages.imagesOf(target.content).forEach { MemoImages.delete(ctx, it) }
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoEditorDialog(
    memo: MemoEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, pinned: Boolean, category: String, priority: Int, place: String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var title by remember(memo?.id) { mutableStateOf(memo?.title.orEmpty()) }
    val initial = remember(memo?.id) { MemoImages.split(memo?.content.orEmpty()) }
    var content by remember(memo?.id) { mutableStateOf(initial.first) }
    var pinned by remember(memo?.id) { mutableStateOf(memo?.pinned ?: false) }
    var aiOpen by remember { mutableStateOf(false) }
    // 0.16.0：与待办同一套日程表单（分类 / 优先级 / 地点 / 识别填写）
    var category by remember(memo?.id) { mutableStateOf(memo?.category ?: "other") }
    var priority by remember(memo?.id) { mutableStateOf(memo?.priority ?: 0) }
    var place by remember(memo?.id) { mutableStateOf(memo?.place.orEmpty()) }
    var aiText by remember { mutableStateOf("") }
    var aiBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun fillFromText() {
        if (aiText.isBlank() || aiBusy) return
        aiBusy = true
        scope.launch {
            val draft = AiMemoClient.organize(ctx, aiText)
            if (draft.title.isNotBlank()) title = draft.title
            if (draft.content.isNotBlank()) content = draft.content
            aiBusy = false
        }
    }
    // 本次编辑里的图片：images = 保存时写进正文的那些；added = 本次新加的（取消时清掉）；removed = 本次删掉的（保存时清掉）
    var images by remember(memo?.id) { mutableStateOf(initial.second) }
    val added = remember(memo?.id) { mutableStateListOf<String>() }
    val removed = remember(memo?.id) { mutableStateListOf<String>() }
    val canSave = title.isNotBlank() || content.isNotBlank() || images.isNotEmpty()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val name = MemoImages.store(ctx, uri)
            if (name != null) {
                images = images + name
                added.add(name)
            } else {
                android.widget.Toast.makeText(ctx, ctx.getString(R.string.memo_image_failed), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun dropImage(name: String) {
        images = images - name
        if (added.remove(name)) MemoImages.delete(ctx, name) else removed.add(name)
    }

    fun cancel() {
        // 取消时本次新加的图片没有任何正文引用它，直接删掉；原有图片保持原样
        added.forEach { MemoImages.delete(ctx, it) }
        onDismiss()
    }

    EditorSheet(
        onDismiss = { cancel() },
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(if (memo == null) R.string.memo_new else R.string.memo_edit),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // AI 整理（0.14.0）：放在标题行的尾部，避免把内容区撑高、把「确定/取消」挤出可视区
                TextButton(onClick = { aiOpen = true }) {
                    Text(stringResource(R.string.memo_ai_button))
                }
            }
        },
        actions = {
            DialogActionButtons(
                confirmText = stringResource(R.string.ok),
                onConfirm = {
                    if (canSave) {
                        removed.forEach { MemoImages.delete(ctx, it) }
                        onSave(title.trim(), MemoImages.join(content, images), pinned, category, priority, place.trim())
                    }
                },
                dismissText = stringResource(R.string.cancel),
                onDismiss = { cancel() },
                confirmEnabled = canSave
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // ① 识别填写：一句话 → 标题 / 正文
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    TextField(
                        value = aiText,
                        onValueChange = { aiText = it },
                        placeholder = { Text(stringResource(R.string.todo_ai_placeholder)) },
                        minLines = 1,
                        maxLines = 3,
                        shape = SleepyTheme.fieldShape,
                        colors = SleepyTheme.fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            R.string.todo_ai_example1,
                            R.string.todo_ai_example2,
                            R.string.todo_ai_example3
                        ).forEach { res ->
                            val sample = stringResource(res)
                            EditorChip(label = sample) { aiText = sample }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { fillFromText() },
                            enabled = aiText.isNotBlank() && !aiBusy,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(if (aiBusy) R.string.todo_ai_working else R.string.todo_ai_fill),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // ② 标题 + 分类
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.memo_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors()
                )
                CategoryChips(value = category, onSelect = { category = it })

                // ③ 地点
                TextField(
                    value = place,
                    onValueChange = { place = it },
                    placeholder = { Text(stringResource(R.string.todo_place_hint)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null, tint = colors.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors()
                )

                // ④ 正文 + 图片
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.noRippleClickable {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.memo_insert_image),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.primary
                        )
                    }
                    images.forEach { name ->
                        MemoThumb(name = name, size = 64.dp, onRemove = { dropImage(name) })
                    }
                }
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.memo_content_label)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors()
                )

                // ⑤ 优先级 + 置顶
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.todo_priority_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    PriorityChips(value = priority, onSelect = { priority = it })
                }
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
            }
        }
    )

    if (aiOpen) {
        AiOrganizeDialog(
            onDismiss = { aiOpen = false },
            onDraft = { draft, usedLocal ->
                if (draft.title.isNotBlank()) title = draft.title
                if (draft.content.isNotBlank()) content = draft.content
                android.widget.Toast.makeText(
                    ctx,
                    ctx.getString(if (usedLocal) R.string.ai_organize_local else R.string.ai_organize_apply),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                aiOpen = false
            }
        )
    }
}

/** 列表里的时间戳：同一年的只给「月-日 时:分」，跨年才带年份，避免每行都写满一串。 */
private fun memoTimeText(millis: Long): String {
    val nowYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    val memoYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(millis))
    val pattern = if (nowYear == memoYear) "MM-dd HH:mm" else "yyyy-MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

}
