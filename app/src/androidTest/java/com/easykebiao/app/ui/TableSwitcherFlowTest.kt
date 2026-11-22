package com.easykebiao.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.easykebiao.app.data.entity.TimeTableEntity
import com.easykebiao.app.ui.screen.schedule.TableSwitcherDialog
import com.easykebiao.app.ui.theme.SleepyThemeProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TableSwitcherFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun currentTableDismissesWithoutResettingWeekAndOtherTableSelects() {
        var dismissed = 0
        val selected = mutableListOf<Long>()
        compose.setContent { SleepyThemeProvider {
            TableSwitcherDialog(listOf(TimeTableEntity(id = 1, name = "Current", startDate = "2026-08-31"), TimeTableEntity(id = 2, name = "Other", startDate = "2026-08-31")),
                1, { selected.add(it) }, { dismissed++ })
        } }
        compose.onNodeWithText("Current").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(1, dismissed); assertTrue(selected.isEmpty()) }
        compose.onNodeWithText("Other").assertIsNotSelected().performClick()
        compose.runOnIdle { assertEquals(listOf(2L), selected) }
    }
}
