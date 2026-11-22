package com.easykebiao.app.ui.screen.mine

import androidx.compose.runtime.saveable.listSaver
import com.easykebiao.app.data.CustomTheme

internal val CustomThemeDraftSaver = listSaver<CustomTheme, Any>(
    save = { listOf(it.id, it.name, it.primary, it.secondary, it.tertiary,
        it.surfaceHue, it.surfaceChroma, it.createdAt) },
    restore = { CustomTheme(it[0] as String, it[1] as String, it[2] as String,
        it[3] as String, it[4] as String, it[5] as Double, it[6] as Double, it[7] as Long) }
)
