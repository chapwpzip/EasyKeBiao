package com.imsx3d.classy.ui.screen.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.imsx3d.classy.ui.component.ConfettiController
import com.imsx3d.classy.ui.component.ConfettiHost
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imsx3d.classy.R
import com.imsx3d.classy.data.entity.TodoEntity
import com.imsx3d.classy.data.entity.TodoStepEntity
import com.imsx3d.classy.ui.component.DialogActionButtons
import com.imsx3d.classy.ui.component.LocalNavExtraBottomPadding
import com.imsx3d.classy.ui.component.applePressScale
import com.imsx3d.classy.ui.component.settingsCard
import com.imsx3d.classy.ui.glass.GlasenseButton
import com.imsx3d.classy.ui.glass.GlasenseButtonColors
import com.imsx3d.classy.ui.glass.shaderRipple
import com.imsx3d.classy.ui.theme.ClassyLayout
import com.imsx3d.classy.ui.theme.SleepyTheme
import com.imsx3d.classy.ui.theme.noRippleClickable
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
                .widthIn(max = ClassyLayout.readingWidth),
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
                    onEdit = { editing = it }
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
            onSave = { title, notes, due, reminder, flagged, pinned, steps ->
                viewModel.save(
                    existing = target,
                    title = title,
                    notes = notes,
                    dueDate = due,
                    reminderAt = reminder,
                    flagged = flagged,
                    pinned = pinned,
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
    onEdit: (TodoEntity) -> Unit
) {
    val overdue = open.filter { it.dueDate != null && it.dueDate < today }
    val todayList = open.filter { it.dueDate == today }
    val upcoming = open.filter { it.dueDate != null && it.dueDate > today }
    val noDate = open.filter { it.dueDate == null }

    fun section(key: String, titleRes: Int, list: List<TodoEntity>) {
        if (list.isEmpty()) return
        item(key = "h-$key") {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = GlasenseTheme.colors.contentVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        }
        items(list, key = { "$key-${it.id}" }) { todo ->
            TodoRow(
                todo = todo,
                today = today,
                steps = stepsByTodo[todo.id].orEmpty(),
                onToggle = { at -> onToggle(todo, at) },
                onClick = { onEdit(todo) },
                modifier = Modifier.animateItem()
            )
        }
    }

    section("overdue", R.string.todo_section_overdue, overdue)
    section("today", R.string.todo_section_today, todayList)
    section("upcoming", R.string.todo_section_upcoming, upcoming)
    section("nodate", R.string.todo_section_nodate, noDate)
}

/** 一条待办：圆形勾选框 + 标题 + 截止/步骤摘要 + 置顶/重要标记。 */
@Composable
private fun TodoRow(
    todo: TodoEntity,
    today: Long,
    steps: List<TodoStepEntity>,
    onToggle: (Offset) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlasenseTheme.colors
    val doneCount = steps.count { it.isDone }
    val overdue = !todo.isCompleted && todo.dueDate != null && todo.dueDate < today
    // 礼花要从这一行的位置冒出来，所以先量下自己在窗口里的中心点
    var rowCenter by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowCenter = it.boundsInRoot().center }
            .clip(RoundedCornerShape(14.dp))
            .settingsCard(colors.cardBackground)
            .applePressScale(0.98f)
            .shaderRipple(dark = isSystemInDarkTheme())
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .noRippleClickable(onClick = { onToggle(rowCenter) }),
            contentAlignment = Alignment.Center
        ) {
            if (todo.isCompleted) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.contentVariant.copy(alpha = 0.45f))
                )
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(colors.cardBackground)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (todo.isCompleted) colors.contentVariant else colors.content,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = buildList {
                todo.dueDate?.let { add(dueText(it, today)) }
                if (steps.isNotEmpty()) {
                    add("$doneCount/${steps.size} " + stringResource(R.string.todo_steps_unit))
                }
                if (todo.notes.isNotBlank()) add(todo.notes.lineSequence().first().take(40))
            }
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = meta.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overdue) colors.error else colors.contentVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (todo.isFlagged) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (todo.isPinned) {
            Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(14.dp)
            )
        }
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
@Composable
private fun TodoEditorDialog(
    todo: TodoEntity?,
    steps: List<TodoStepEntity>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (String, String, Long?, Long?, Boolean, Boolean, List<Pair<String, Boolean>>) -> Unit
) {
    val colors = GlasenseTheme.colors
    val context = LocalContext.current
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (todo == null) R.string.todo_new else R.string.todo_edit)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.memo_title_label)) },
                    singleLine = true,
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text(stringResource(R.string.todo_notes_label)) },
                    minLines = 2,
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    DueChip(stringResource(R.string.todo_due_today), due == today) { due = today }
                    Spacer(modifier = Modifier.width(6.dp))
                    DueChip(stringResource(R.string.todo_due_tomorrow), due == today + 1) { due = today + 1 }
                    Spacer(modifier = Modifier.width(6.dp))
                    DueChip(stringResource(R.string.todo_due_clear), due == null) { due = null }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DueChip(
                        label = reminder?.let {
                            stringResource(R.string.todo_reminder_at, reminderText(it))
                        } ?: stringResource(R.string.todo_reminder_set),
                        selected = reminder != null
                    ) {
                        pickReminder(context, reminder) { reminder = it; warnIfNotificationsOff(context) }
                    }
                    if (reminder != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        DueChip(stringResource(R.string.todo_reminder_clear), selected = false) {
                            reminder = null
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    ToggleChip(stringResource(R.string.memo_pin), pinned) { pinned = it }
                    Spacer(modifier = Modifier.width(6.dp))
                    ToggleChip(stringResource(R.string.todo_flag), flagged, tint = colors.error) { flagged = it }
                }
                Spacer(modifier = Modifier.height(12.dp))
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
        confirmButton = {
            DialogActionButtons(
                confirmText = stringResource(R.string.save),
                onConfirm = {
                    onSave(title.trim(), notes.trim(), due, reminder, flagged, pinned, stepItems.toList())
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