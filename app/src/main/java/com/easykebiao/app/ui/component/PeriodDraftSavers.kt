package com.easykebiao.app.ui.component

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.easykebiao.app.data.entity.SmartPeriodConfig
import com.easykebiao.app.util.TimeTableUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Saves unfinished input verbatim; restoration never writes to the database. */
val PeriodRowsSaver = Saver<SnapshotStateList<TimeTableUtils.TimeSlotRow>, String>(
    save = { TimeTableUtils.buildTimeJsonFromRows(it.toList()) },
    restore = { json -> mutableStateListOf<TimeTableUtils.TimeSlotRow>().apply {
        addAll(TimeTableUtils.parseTimeSlotRows(json))
    } }
)

val SmartPeriodDraftSaver = Saver<SmartPeriodConfig, String>(
    save = { Json.encodeToString(it) },
    restore = { decodeSmartPeriodConfig(it) }
)
