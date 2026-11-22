package com.easykebiao.app.ui.screen.mine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.easykebiao.app.R
import com.easykebiao.app.ui.component.SettingsGroupCard
import com.easykebiao.app.ui.component.SettingsGroupRow
import com.easykebiao.app.ui.component.SettingsRowDivider
import com.easykebiao.app.ui.component.SettingsScaffold

/** Choose a topic first; individual preferences live in their existing pages. */
@Composable
fun SettingsHomeScreen(
    onBack: () -> Unit,
    onOpenScheduleDisplay: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenWeather: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},
) {
    SettingsScaffold(title = stringResource(R.string.settings_home_title), onBack = onBack) {
        item {
            SettingsGroupCard {
                SettingsTopic(R.string.schedule_display_title, onOpenScheduleDisplay)
                SettingsRowDivider()
                SettingsTopic(R.string.mine_general, onOpenGeneral)
                SettingsRowDivider()
                SettingsTopic(R.string.weather_title, onOpenWeather)
                SettingsRowDivider()
                SettingsTopic(R.string.ai_title, onOpenAiSettings)
            }
        }
    }
}

@Composable
private fun SettingsTopic(title: Int, onClick: () -> Unit) {
    SettingsGroupRow(title = stringResource(title), onClick = onClick, trailing = {
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
    })
}
