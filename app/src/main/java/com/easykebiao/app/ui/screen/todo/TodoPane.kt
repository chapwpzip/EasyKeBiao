package com.easykebiao.app.ui.screen.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.easykebiao.app.data.ai.AiMemoClient
import com.easykebiao.app.ui.component.CategoryChips
import com.easykebiao.app.ui.component.PriorityChips
import com.easykebiao.app.ui.component.PriorityLabel
import com.easykebiao.app.ui.component.EditorChip
import com.easykebiao.app.ui.component.EditorSheet
import com.easykebiao.app.ui.component.TodoCategory
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import com.easykebiao.app.ui.component.ConfettiController
import com.easykebiao.app.ui.component.ConfettiHost
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.easykebiao.app.R
import com.easykebiao.app.data.entity.TodoEntity
import com.easykebiao.app.data.memo.MemoImages
import com.easykebiao.app.ui.component.MemoThumb
import com.easykebiao.app.data.entity.TodoStepEntity
import com.easykebiao.app.ui.component.DialogActionButtons
import com.easykebiao.app.ui.component.LocalNavExtraBottomPadding
import com.easykebiao.app.ui.component.applePressScale
import com.easykebiao.app.ui.component.settingsCard
import com.easykebiao.app.ui.glass.GlasenseButton
import com.easykebiao.app.ui.glass.GlasenseButtonColors
import com.easykebiao.app.ui.glass.shaderRipple
import com.easykebiao.app.ui.theme.AppLayout
import com.easykebiao.app.ui.theme.SleepyTheme
import com.easykebiao.app.ui.theme.noRippleClickable
import com.kyant.shapes.Capsule
import com.nevoit.glasense.theme.GlasenseTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 「待办」段。分组固定四段（已逾期 / 今天 / 之后 / 无日期）+ 可折叠的已完成，
 * 不做自定义排序、不做标签、不做重复规则 —— 一屏看完，不加认知负担。
 */
@Composable
fun TodoPane(
    modifier: Modifier = Modifier,
    viewModel: TodoViewModel = viewModel()
) {
    val todos by viewModel.todos.collectAsState()
    val stepsByTodo by viewModel.stepsByTodo.collectAsState()
    val colors = GlasenseTheme.colors

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var showDone by remember { mutableStateOf(false) }

    // 礼花：勾掉一条时在那一行撒一把。overlay 原点用于把行坐标换算成 overlay 局部坐标。
    val confetti = remember { ConfettiController() }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    val toggleWithConfetti: (TodoEntity, Offset) -> Unit = { todo, at ->
        val wasOpen = !todo.isCompleted
        viewModel.toggleCompleted(todo)
        if (wasOpen) confetti.burst(at - overlayOrigin)
    }

    val today = remember { LocalDate.now().toEpochDay() }
    val open = todos.filter { !it.isCompleted }
    val done = todos.filter { it.isCompleted }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() }
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = AppLayout.readingWidth),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 16.dp + LocalNavExtraBottomPadding.current
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (todos.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.todo_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.contentVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            } else {
                todoSections(
                    open = open,
                    today = today,
                    stepsByTodo = stepsByTodo,
                    onToggle = toggleWithConfetti,
                    onEdit = { editing = it },
                    overdueAccent = colors.error,
                    todayAccent = colors.primary,
                    laterAccent = colors.contentVariant.copy(alpha = 0.55f)
                )
                if (done.isNotEmpty()) {
                    item(key = "done-header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .noRippleClickable { showDone = !showDone }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.todo_section_done) + " ${done.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.contentVariant
                            )
                        }
                    }
                    if (showDone) {
                        items(done, key = { "done-${it.id}" }) { todo ->
                            TodoRow(
                                todo = todo,
                                today = today,
                                steps = stepsByTodo[todo.id].orEmpty(),
                                onToggle = { at -> toggleWithConfetti(todo, at) },
                                onClick = { editing = todo },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }

        GlasenseButton(
            onClick = { creating = true },
            modifier = Modifier
                .fillMaxWidth()
                // 悬浮 Dock 会压住底部：多留一个 Dock 的高度，按钮才在玻璃条上方
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 10.dp + LocalNavExtraBottomPadding.current
                ),
            shape = Capsule(),
            colors = GlasenseButtonColors(
                containerColor = colors.primary,
                contentColor = Color.White,
                disabledContainerColor = colors.primary.copy(alpha = 0.45f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.todo_new), color = Color.White)
            }
        }
        }

        // 礼花层：盖在列表之上、不拦点击（Canvas 不消费触摸）
        ConfettiHost(confetti, Modifier.matchParentSize())
    }

    if (creating || editing != null) {
        val target = editing
        TodoEditorDialog(
            todo = target,
            steps = target?.let { stepsByTodo[it.id].orEmpty() }.orEmpty(),
            onDismiss = { creating = false; editing = null },
            onDelete = target?.let { t -> { viewModel.delete(t) } },
            onSave = { title, notes, due, reminder, flagged, pinned, steps, image, category, priority, place ->
                viewModel.save(
                    existing = target,
                    title = title,
                    notes = notes,
                    dueDate = due,
                    reminderAt = reminder,
                    flagged = flagged,
                    pinned = pinned,
                    imagePath = image,
                    category = category,
                    priority = priority,
                    place = place,
                    steps = steps
                )
                creating = false
                editing = null
            }
        )
    }
}

/** 四段未完成列表，空的段不画。 */
private fun LazyListScope.todoSections(
    open: List<TodoEntity>,
    today: Long,
    stepsByTodo: Map<Long, List<TodoStepEntity>>,
    onToggle: (TodoEntity, Offset) -> Unit,
    onEdit: (TodoEntity) -> Unit,
    overdueAccent: Color,
    todayAccent: Color,
    laterAccent: Color
) {
    val overdue = open.filter { it.dueDate != null && it.dueDate < today }
    val todayList = open.filter { it.dueDate == today }
    val upcoming = open.filter { it.dueDate != null && it.dueDate > today }
    val noDate = open.filter { it.dueDate == null }

    fun section(key: String, titleRes: Int, accent: Color, list: List<TodoEntity>) {
        if (list.isEmpty()) return
        item(key = "h-$key") {
            TodoSectionHeader(
                title = stringResource(titleRes),
                count = list.size,
                accent = accent
            )
        }
        itemsIndexed(list, key = { _, todo -> "$key-${todo.id}" }) { index, todo ->
            TodoRow(
                todo = todo,
                today = today,
                steps = stepsByTodo[todo.id].orEmpty(),
                onToggle = { at -> onToggle(todo, at) },
                onClick = { onEdit(todo) },
                first = index == 0,
                last = index == list.lastIndex,
                modifier = Modifier.animateItem()
            )
        }
    }

    section("overdue", R.string.todo_section_overdue, overdueAccent, overdue)
    section("today", R.string.todo_section_today, todayAccent, todayList)
    section("upcoming", R.string.todo_section_upcoming, laterAccent, upcoming)
    section("nodate", R.string.todo_section_nodate, laterAccent, noDate)
}

/** 分组标题：一个小圆点定色，右侧跟条数（0.15.0 起，和备忘的 inset 分组同一套节奏）。 */
@Composable
private fun TodoSectionHeader(title: String, count: Int, accent: Color) {
    val colors = GlasenseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = colors.contentVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.contentVariant.copy(alpha = 0.65f)
        )
    }
}

/**
 * 一条待办（0.15.0 重做视觉）：描边圆环勾选框 + 标题 + 截止胶囊 + 步骤进度。
 *
 * 同一分组内的行共用一张卡片：首行圆上沿、末行圆下沿、中间直角并用发丝线分隔，
 * 和「备忘」列表是同一种 inset-grouped 语汇。
 */
@Composable
private fun TodoRow(
    todo: TodoEntity,
    today: Long,
    steps: List<TodoStepEntity>,
    onToggle: (Offset) -> Unit,
    onClick: () -> Unit,
    first: Boolean = true,
    last: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = GlasenseTheme.colors
    val doneCount = steps.count { it.isDone }
    val overdue = !todo.isCompleted && todo.dueDate != null && todo.dueDate < today
    val dueToday = !todo.isCompleted && todo.dueDate == today
    // 礼花要从这一行的位置冒出来，所以先量下自己在窗口里的中心点
    var rowCenter by remember { mutableStateOf(Offset.Zero) }
    val r = 14.dp
    val shape = RoundedCornerShape(
        topStart = if (first) r else 0.dp,
        topEnd = if (first) r else 0.dp,
        bottomStart = if (last) r else 0.dp,
        bottomEnd = if (last) r else 0.dp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowCenter = it.boundsInRoot().center }
            .clip(shape)
            .settingsCard(colors.cardBackground)
            .applePressScale(0.99f)
            .shaderRipple(dark = isSystemInDarkTheme())
            .noRippleClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = if (last) 13.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 勾选框：未完成是描边环（比实心块安静），完成后才填充主色
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .noRippleClickable(onClick = { onToggle(rowCenter) }),
                contentAlignment = Alignment.Center
            ) {
                if (todo.isCompleted) {
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(colors.primary))
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.cardBackground)
                            .border(
                                width = 1.6.dp,
                                color = when {
                                    overdue -> colors.error.copy(alpha = 0.55f)
                                    todo.category != TodoCategory.TODO -> TodoCategory.color(todo.category).copy(alpha = 0.75f)
                                    else -> colors.contentVariant.copy(alpha = 0.45f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            todo.imagePath?.let { name ->
                MemoThumb(name = name, size = 44.dp, radius = 9.dp)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (todo.isCompleted) colors.contentVariant else colors.content,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val hasMeta = todo.dueDate != null || steps.isNotEmpty() || todo.place.isNotBlank() || todo.priority > 0
                if (hasMeta) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        todo.dueDate?.let { due ->
                            DuePill(
                                text = dueText(due, today),
                                tone = when {
                                    todo.isCompleted -> DueTone.Done
                                    overdue -> DueTone.Overdue
                                    dueToday -> DueTone.Today
                                    else -> DueTone.Later
                                }
                            )
                        }
                        if (steps.isNotEmpty()) {
                            if (todo.dueDate != null) Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$doneCount/${steps.size} " + stringResource(R.string.todo_steps_unit),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.contentVariant
                            )
                        }
                        if (todo.priority > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = when (todo.priority) {
                                    3 -> colors.error
                                    2 -> androidx.compose.ui.graphics.Color(0xFFFF9500)
                                    else -> TodoCategory.color(TodoCategory.ACTIVITY)
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = PriorityLabel(todo.priority),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.contentVariant
                            )
                        }
                        if (todo.place.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Outlined.Place,
                                contentDescription = null,
                                tint = colors.contentVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = todo.place,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.contentVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (todo.isCompleted.not() && todo.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = todo.notes.lineSequence().first().take(48),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.contentVariant.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (todo.isFlagged) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = colors.error.copy(alpha = 0.9f),
                    modifier = Modifier.size(15.dp)
                )
            }
            if (todo.isPinned) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (!last) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            )
        }
    }
}

/** 截止日期胶囊：逾期红、今天蓝、以后灰，完成态一律淡掉。 */
private enum class DueTone { Overdue, Today, Later, Done }

@Composable
private fun DuePill(text: String, tone: DueTone) {
    val colors = GlasenseTheme.colors
    val fg = when (tone) {
        DueTone.Overdue -> colors.error
        DueTone.Today -> colors.primary
        DueTone.Later -> colors.contentVariant
        DueTone.Done -> colors.contentVariant.copy(alpha = 0.7f)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(fg.copy(alpha = if (tone == DueTone.Done) 0.06f else 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg
        )
    }
}

/** 截止日文案：今天 / 明天 / 昨天 / N 天后 / M月d日。 */
@Composable
private fun dueText(epochDay: Long, today: Long): String {
    val diff = epochDay - today
    return when (diff) {
        0L -> stringResource(R.string.todo_due_today)
        1L -> stringResource(R.string.todo_due_tomorrow)
        -1L -> stringResource(R.string.todo_due_yesterday)
        2L, 3L, 4L, 5L, 6L -> stringResource(R.string.todo_days_after, diff)
        else -> LocalDate.ofEpochDay(epochDay)
            .format(DateTimeFormatter.ofPattern(stringResource(R.string.todo_date_pattern), Locale.getDefault()))
    }
}

/** 新建/编辑待办：标题、备注、截止（今天/明天/清除）、置顶/重要、步骤清单。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodoEditorDialog(
    todo: TodoEntity?,
    steps: List<TodoStepEntity>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (String, String, Long?, Long?, Boolean, Boolean, List<Pair<String, Boolean>>, String?, String, Int, String) -> Unit
) {
    val colors = GlasenseTheme.colors
    val context = LocalContext.current
    // 附图（0.15.0）：一条待办配一张图，存文件名，列表与桌面组件都显示缩略图
    var image by remember(todo?.id) { mutableStateOf(todo?.imagePath) }
    var addedImage by remember(todo?.id) { mutableStateOf<String?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val name = MemoImages.store(context, uri)
            if (name != null) {
                addedImage?.let { MemoImages.delete(context, it) }
                image = name
                addedImage = name
            } else {
                android.widget.Toast.makeText(context, context.getString(R.string.memo_image_failed), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    var title by remember { mutableStateOf(todo?.title.orEmpty()) }
    var notes by remember { mutableStateOf(todo?.notes.orEmpty()) }
    var due by remember { mutableStateOf(todo?.dueDate) }
    var reminder by remember { mutableStateOf(todo?.reminderAt) }
    var flagged by remember { mutableStateOf(todo?.isFlagged ?: false) }
    var pinned by remember { mutableStateOf(todo?.isPinned ?: false) }
    val stepItems = remember {
        mutableStateListOf<Pair<String, Boolean>>().also { list ->
            steps.forEach { list.add(it.title to it.isDone) }
        }
    }
    var stepDraft by remember { mutableStateOf("") }
    val today = remember { LocalDate.now().toEpochDay() }
    // 0.16.0：日程式表单 —— 分类 / 优先级 / 地点，以及顶部的「识别填写」
    var category by remember(todo?.id) { mutableStateOf(todo?.category ?: TodoCategory.TODO) }
    var priority by remember(todo?.id) { mutableStateOf(todo?.priority ?: 0) }
    var place by remember(todo?.id) { mutableStateOf(todo?.place.orEmpty()) }
    var aiText by remember { mutableStateOf("") }
    var aiBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun fillFromText() {
        if (aiText.isBlank() || aiBusy) return
        aiBusy = true
        scope.launch {
            val draft = AiMemoClient.organize(context, aiText)
            if (draft.title.isNotBlank()) title = draft.title
            if (draft.content.isNotBlank()) notes = draft.content
            draft.dueDate?.let { due = it.toEpochDay() }
            val day = draft.dueDate ?: due?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
            draft.time?.split(":")?.let { parts ->
                val h = parts.getOrNull(0)?.toIntOrNull()
                val m = parts.getOrNull(1)?.toIntOrNull()
                if (h != null && m != null) {
                    reminder = day.atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
            aiBusy = false
        }
    }
    EditorSheet(
        onDismiss = onDismiss,
        header = { Text(stringResource(if (todo == null) R.string.todo_new else R.string.todo_edit)) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ① 识别填写：一句话 → 标题 / 备注 / 截止 / 时间
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.contentVariant.copy(alpha = 0.06f))
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
                    // 示例与「识别填写」挤在同一行：示例横向滚动，按钮常驻右侧
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
                    placeholder = { Text(stringResource(R.string.todo_title_hint)) },
                    singleLine = true,
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryChips(value = category, onSelect = { category = it })

                // ③ 全天 / 截止 / 时间
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.todo_allday),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.content
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = reminder == null && due != null,
                        onCheckedChange = { allDay ->
                            if (allDay) {
                                reminder = null
                                if (due == null) due = today
                            }
                        }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.todo_due_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.contentVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    EditorChip(stringResource(R.string.todo_due_today), due == today) { due = today }
                    Spacer(modifier = Modifier.width(6.dp))
                    EditorChip(stringResource(R.string.todo_due_tomorrow), due == today + 1) { due = today + 1 }
                    Spacer(modifier = Modifier.width(6.dp))
                    EditorChip(stringResource(R.string.todo_due_clear), due == null) { due = null }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.todo_time_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.contentVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    EditorChip(
                        label = reminder?.let { reminderText(it) } ?: stringResource(R.string.todo_time_clear),
                        selected = reminder != null
                    ) {
                        pickReminder(context, reminder) { reminder = it; warnIfNotificationsOff(context) }
                    }
                }

                // ④ 地点
                TextField(
                    value = place,
                    onValueChange = { place = it },
                    placeholder = { Text(stringResource(R.string.todo_place_hint)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null, tint = colors.contentVariant) },
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // ⑤ 备注
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text(stringResource(R.string.todo_notes_placeholder)) },
                    minLines = 2,
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // ⑥ 优先级
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.todo_priority_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.content
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    PriorityChips(value = priority, onSelect = { priority = it })
                }

                // ⑦ 附图 + 置顶
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .noRippleClickable {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                            text = stringResource(R.string.todo_insert_image),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.primary
                        )
                    }
                    image?.let { name ->
                        Spacer(modifier = Modifier.width(12.dp))
                        MemoThumb(
                            name = name,
                            size = 56.dp,
                            onRemove = {
                                MemoImages.delete(context, name)
                                if (addedImage == name) addedImage = null
                                image = null
                            }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    ToggleChip(stringResource(R.string.memo_pin), pinned) { pinned = it }
                }

                // ⑧ 子步骤
                stepItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .noRippleClickable { stepItems[index] = item.first to !item.second }
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = if (item.second) colors.primary else colors.contentVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.first,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.second) colors.contentVariant else colors.content,
                            textDecoration = if (item.second) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { stepItems.removeAt(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = colors.contentVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = stepDraft,
                        onValueChange = { stepDraft = it },
                        placeholder = { Text(stringResource(R.string.todo_step_hint)) },
                        singleLine = true,
                        shape = SleepyTheme.fieldShape,
                        colors = SleepyTheme.fieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = stepDraft.isNotBlank(),
                        onClick = {
                            stepItems.add(stepDraft.trim() to false)
                            stepDraft = ""
                        }
                    ) { Text(stringResource(R.string.todo_step_add)) }
                }
            }
        },
        actions = {
            DialogActionButtons(
                confirmText = stringResource(if (todo == null) R.string.todo_create else R.string.save),
                onConfirm = {
                    onSave(
                        title.trim(), notes.trim(), due, reminder, priority >= 2, pinned,
                        stepItems.toList(), image, category, priority, place.trim()
                    )
                },
                dismissText = stringResource(R.string.cancel),
                onDismiss = onDismiss,
                thirdText = if (onDelete == null) null else stringResource(R.string.memo_delete),
                onThird = { onDelete?.invoke(); onDismiss() },
                thirdDestructive = true,
                confirmEnabled = title.isNotBlank() || stepItems.isNotEmpty()
            )
        }
    )
}
@Composable
private fun DueChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = GlasenseTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) colors.primary.copy(alpha = 0.16f) else colors.contentVariant.copy(alpha = 0.10f))
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.primary else colors.contentVariant
        )
    }
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    tint: Color = GlasenseTheme.colors.primary,
    onClick: (Boolean) -> Unit
) {
    val colors = GlasenseTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) tint.copy(alpha = 0.16f) else colors.contentVariant.copy(alpha = 0.10f))
            .noRippleClickable { onClick(!selected) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) tint else colors.contentVariant
        )
    }
}

/** 提醒时刻的显示：今天 / 明天 / 具体日期 + 时:分。 */
@Composable
private fun reminderText(millis: Long): String {
    val zone = remember { ZoneId.systemDefault() }
    val dt = remember(millis) { Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime() }
    val clock = String.format(Locale.getDefault(), "%02d:%02d", dt.hour, dt.minute)
    val today = LocalDate.now()
    return when (dt.toLocalDate()) {
        today -> stringResource(R.string.todo_due_today) + " " + clock
        today.plusDays(1) -> stringResource(R.string.todo_due_tomorrow) + " " + clock
        else -> dt.format(DateTimeFormatter.ofPattern(stringResource(R.string.todo_reminder_pattern), Locale.getDefault())) + " " + clock
    }
}

/**
 * 选提醒时刻：先日期后时间（系统原生两个对话框）。
 *
 * 没设过就给一个省事的默认值 —— 20 点后默认明天 9:00，否则下一个整点。
 */
private fun pickReminder(context: Context, initial: Long?, onPicked: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    val now = LocalDateTime.now()
    val base = initial?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
        ?: run {
            if (now.hour >= 20) LocalDate.now().plusDays(1).atTime(LocalTime.of(9, 0))
            else now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
        }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    onPicked(
                        LocalDateTime.of(year, month + 1, day, hour, minute)
                            .atZone(zone).toInstant().toEpochMilli()
                    )
                },
                base.hour,
                base.minute,
                true
            ).show()
        },
        base.year,
        base.monthValue - 1,
        base.dayOfMonth
    ).show()
}

/** 通知被用户关掉时给一句提醒，否则设了闹钟到点没反应会以为坏了。 */
private fun warnIfNotificationsOff(context: Context) {
    val enabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    if (!enabled) {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.todo_reminder_need_permission),
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}