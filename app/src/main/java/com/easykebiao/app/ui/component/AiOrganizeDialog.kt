package com.easykebiao.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.easykebiao.app.R
import com.easykebiao.app.data.ai.AiMemoClient
import com.easykebiao.app.data.ai.MemoDraft
import com.easykebiao.app.data.ai.LocalMemoRules
import com.easykebiao.app.ui.theme.SleepyTheme
import kotlinx.coroutines.launch

/**
 * 「AI 整理」弹窗（0.14.0，2026-11-22）。
 *
 * 粘贴一段话 → 整理 → 回填到备忘编辑框（**不直接写库**，用户还能改）。
 * 没配 key、模型失败、返回不是 JSON —— 一律退回本地规则，并用一行小字如实说明来源。
 */
@Composable
fun AiOrganizeDialog(
    onDismiss: () -> Unit,
    onDraft: (MemoDraft, Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.ai_organize)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.ai_organize_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SleepyTheme.fieldShape,
                    colors = SleepyTheme.fieldColors(),
                    label = { Text(stringResource(R.string.memo_content_label)) }
                )
                Text(
                    text = stringResource(R.string.ai_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank() && !busy,
                onClick = {
                    if (busy) return@TextButton
                    busy = true
                    scope.launch {
                        val draft = runCatching { AiMemoClient.organize(ctx, input) }
                            .getOrElse { LocalMemoRules.organize(input) }
                        busy = false
                        onDraft(draft, draft.source == MemoDraft.Source.LOCAL)
                    }
                }
            ) {
                Text(
                    if (busy) stringResource(R.string.ai_organize_running)
                    else stringResource(R.string.ai_organize)
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
