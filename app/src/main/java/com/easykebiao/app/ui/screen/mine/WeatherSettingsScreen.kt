package com.easykebiao.app.ui.screen.mine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.easykebiao.app.data.weather.CityCatalog
import com.easykebiao.app.data.weather.WeatherCity
import com.easykebiao.app.data.weather.WeatherRepository
import com.easykebiao.app.ui.component.SectionHeader
import com.easykebiao.app.ui.component.SettingsGroupCard
import com.easykebiao.app.ui.component.SettingsGroupRow
import com.easykebiao.app.ui.component.SettingsNoteRow
import com.easykebiao.app.ui.component.SettingsRowDivider
import com.easykebiao.app.ui.component.SettingsScaffold
import com.easykebiao.app.ui.component.SettingsSliderRow
import com.easykebiao.app.ui.component.SettingsSwitchRow
import com.easykebiao.app.util.AppPrefs
import kotlinx.coroutines.launch

/**
 * 天气设置（0.14.0，2026-11-22）。
 *
 * 三块：显示开关 + 城市；上课天气提醒（阈值两个滑杆）；数据来源说明。
 * 城市是**内置表手选**，不申请定位权限 —— 用户没选就默认桂林，随时可改。
 */
@Composable
fun WeatherSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(AppPrefs.isWeatherEnabled(ctx)) }
    var city by remember { mutableStateOf(AppPrefs.getWeatherCity(ctx)) }
    var alertEnabled by remember { mutableStateOf(AppPrefs.isWeatherAlertEnabled(ctx)) }
    var pct by remember { mutableStateOf(AppPrefs.getWeatherAlertPct(ctx).toFloat()) }
    var drop by remember { mutableStateOf(AppPrefs.getWeatherAlertDrop(ctx).toFloat()) }
    var showPicker by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    SettingsScaffold(title = stringResource(R.string.weather_title), onBack = onBack) {
        item {
            SettingsGroupCard {
                SettingsSwitchRow(
                    title = stringResource(R.string.weather_show_title),
                    subtitle = stringResource(R.string.weather_show_sub),
                    checked = enabled,
                    onCheckedChange = { v ->
                        enabled = v
                        AppPrefs.setWeatherEnabled(ctx, v)
                        if (v) {
                            busy = true
                            scope.launch {
                                val snap = WeatherRepository.refreshIfStale(ctx, force = true)
                                busy = false
                                status = if (snap == null) {
                                    ctx.getString(R.string.weather_fetch_failed)
                                } else {
                                    ctx.getString(R.string.weather_fetched_at, snap.city)
                                }
                            }
                        }
                    }
                )
                SettingsRowDivider()
                SettingsGroupRow(
                    title = stringResource(R.string.weather_city),
                    subtitle = city.name,
                    onClick = { showPicker = true }
                )
                SettingsRowDivider()
                SettingsGroupRow(
                    title = stringResource(R.string.weather_refresh_now),
                    subtitle = status ?: stringResource(R.string.weather_refresh_hint),
                    onClick = {
                        if (busy) return@SettingsGroupRow
                        busy = true
                        scope.launch {
                            val snap = WeatherRepository.refreshIfStale(ctx, force = true)
                            busy = false
                            status = if (snap == null) {
                                ctx.getString(R.string.weather_fetch_failed)
                            } else {
                                ctx.getString(R.string.weather_fetched_at, snap.city)
                            }
                        }
                    }
                )
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.weather_alert_section),
                subtitle = stringResource(R.string.weather_alert_section_sub)
            )
        }
        item {
            SettingsGroupCard {
                SettingsSwitchRow(
                    title = stringResource(R.string.weather_alert_title),
                    subtitle = stringResource(R.string.weather_alert_sub),
                    checked = alertEnabled,
                    onCheckedChange = { v ->
                        alertEnabled = v
                        AppPrefs.setWeatherAlertEnabled(ctx, v)
                    }
                )
                if (alertEnabled) {
                    SettingsRowDivider()
                    SettingsSliderRow(
                        title = stringResource(R.string.weather_alert_pct),
                        subtitle = stringResource(R.string.weather_alert_pct_sub),
                        valueText = "${pct.toInt()}%",
                        value = pct,
                        valueRange = 10f..100f,
                        onValueChange = { pct = it },
                        onValueChangeFinished = { AppPrefs.setWeatherAlertPct(ctx, pct.toInt()) }
                    )
                    SettingsRowDivider()
                    SettingsSliderRow(
                        title = stringResource(R.string.weather_alert_drop),
                        subtitle = stringResource(R.string.weather_alert_drop_sub),
                        valueText = "≥ ${drop.toInt()}℃",
                        value = drop,
                        valueRange = 1f..20f,
                        onValueChange = { drop = it },
                        onValueChangeFinished = { AppPrefs.setWeatherAlertDrop(ctx, drop.toInt()) }
                    )
                }
            }
        }
        item { SettingsNoteRow(stringResource(R.string.weather_alert_note)) }
        item { SettingsNoteRow(stringResource(R.string.weather_source_note)) }
    }

    if (showPicker) {
        CityPickerDialog(
            selected = city.name,
            onPick = { picked ->
                showPicker = false
                if (picked.name != city.name) {
                    city = picked
                    AppPrefs.setWeatherCity(ctx, picked)
                    WeatherRepository.forget()
                    status = null
                    scope.launch {
                        val snap = WeatherRepository.refreshIfStale(ctx, force = true)
                        status = if (snap == null) {
                            ctx.getString(R.string.weather_fetch_failed)
                        } else {
                            ctx.getString(R.string.weather_fetched_at, snap.city)
                        }
                    }
                }
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun CityPickerDialog(
    selected: String,
    onPick: (WeatherCity) -> Unit,
    onDismiss: () -> Unit
) {
    val cities = remember { CityCatalog.all }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weather_city_picker_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(cities, key = { it.name }) { c ->
                        SettingsGroupRow(
                            title = c.name,
                            subtitle = if (c.name == selected) {
                                stringResource(R.string.weather_city_current)
                            } else null,
                            onClick = { onPick(c) },
                            modifier = Modifier.fillMaxWidth(),
                            horizontalPadding = 4.dp
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.weather_city_picker_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    )
}
