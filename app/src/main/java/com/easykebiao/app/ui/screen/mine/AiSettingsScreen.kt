package com.easykebiao.app.ui.screen.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.easykebiao.app.R
import com.easykebiao.app.data.ai.AiMemoClient
import com.easykebiao.app.ui.component.SettingsGroupCard
import com.easykebiao.app.ui.component.SettingsNoteRow
import com.easykebiao.app.ui.component.SettingsRowDivider
import com.easykebiao.app.ui.component.SettingsScaffold
import com.easykebiao.app.ui.theme.SleepyTheme
import com.easykebiao.app.util.AppPrefs
import kotlinx.coroutines.launch

/**
 * AI 接口设置（BYOK，0.14.0）。
 *
 * 用户自己填 baseUrl / model / key；三者都只留在本机 SharedPreferences。
 * 不填 key 也能用：备忘里的「AI 整理」会走本地规则（见 LocalMemoRules）。
 */
@Composable
fun AiSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var base by remember { mutableStateOf(AppPrefs.getAiBaseUrl(ctx)) }
    var model by remember { mutableStateOf(AppPrefs.getAiModel(ctx)) }
    var key by remember { mutableStateOf(AppPrefs.getAiApiKey(ctx)) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }

    SettingsScaffold(title = stringResource(R.string.ai_title), onBack = onBack) {
        item {
            SettingsGroupCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        value = base,
                        onValueChange = {
                            base = it
                            AppPrefs.setAiBaseUrl(ctx, it)
                        },
                        label = { Text(stringResource(R.string.ai_base_url)) },
                        placeholder = { Text(stringResource(R.string.ai_base_url_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SleepyTheme.fieldShape,
                        colors = SleepyTheme.fieldColors()
                    )
                    TextField(
                        value = model,
                        onValueChange = {
                            model = it
                            AppPrefs.setAiModel(ctx, it)
                        },
                        label = { Text(stringResource(R.string.ai_model)) },
                        placeholder = { Text(stringResource(R.string.ai_model_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SleepyTheme.fieldShape,
                        colors = SleepyTheme.fieldColors()
                    )
                    TextField(
                        value = key,
                        onValueChange = {
                            key = it
                            AppPrefs.setAiApiKey(ctx, it)
                        },
                        label = { Text(stringResource(R.string.ai_key)) },
                        placeholder = { Text(stringResource(R.string.ai_key_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = SleepyTheme.fieldShape,
                        colors = SleepyTheme.fieldColors()
                    )
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            result = null
                            scope.launch {
                                val r = AiMemoClient.testConnection(ctx)
                                busy = false
                                ok = r.isSuccess
                                result = r.fold(
                                    onSuccess = { ctx.getString(R.string.ai_test_ok) },
                                    onFailure = { ctx.getString(R.string.ai_test_fail, it.message ?: "unknown") }
                                )
                            }
                        }
                    ) {
                        Text(
                            if (busy) stringResource(R.string.ai_test_running)
                            else stringResource(R.string.ai_test)
                        )
                    }
                    if (result != null) {
                        Text(
                            text = result.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ok) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        item { SettingsRowDivider() }
        item { SettingsNoteRow(stringResource(R.string.ai_note)) }
    }
}
