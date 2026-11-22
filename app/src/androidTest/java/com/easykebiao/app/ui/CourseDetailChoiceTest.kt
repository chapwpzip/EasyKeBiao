package com.easykebiao.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.easykebiao.app.R
import com.easykebiao.app.data.entity.CourseEntity
import com.easykebiao.app.ui.component.CourseDetailSheet
import com.easykebiao.app.ui.theme.SleepyThemeProvider
import com.easykebiao.app.util.AppPrefs
import com.easykebiao.app.util.ConflictLayoutEngine
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CourseDetailChoiceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun defaultTopUsesExplicitAutomaticChoiceAndSelectedClickIsIdempotent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = CourseEntity(id = 81001, groupId = "choice-a", tableId = 81000,
            courseName = "First overlap course", day = 2, startNode = 1, step = 2,
            startWeek = 1, endWeek = 16, color = "#123456")
        val second = first.copy(id = 81002, groupId = "choice-b", courseName = "Second overlap course")
        val rows = listOf(first, second)
        val cluster = ConflictLayoutEngine.findClusters(rows, null).single()
        val key = ConflictLayoutEngine.conflictClusterKey(cluster)
        val previous = AppPrefs.getConflictDefaultTop(context)[key]
        val automatic = context.getString(R.string.conflict_default_top_auto)
        val writes = mutableListOf<Long?>()
        try {
            AppPrefs.putConflictDefaultTop(context, key, null)
            compose.setContent {
                SleepyThemeProvider {
                    CourseDetailSheet(first, allCourses = rows, onDismiss = {}, onDefaultTopChanged = { changedKey, id ->
                        assertEquals(key, changedKey)
                        writes.add(id)
                        AppPrefs.putConflictDefaultTop(context, changedKey, id)
                    })
                }
            }
            compose.onNode(hasText(automatic) and isSelectable()).assertIsSelected()
            val choice = compose.onNode(hasText(second.courseName) and isSelectable())
            choice.performScrollTo().performClick()
            choice.assertIsSelected()
            choice.performClick()
            compose.runOnIdle { assertEquals(listOf(second.id), writes) }
            compose.onNode(hasText(automatic) and isSelectable()).performScrollTo().performClick().assertIsSelected()
            compose.runOnIdle { assertEquals(listOf(second.id, null), writes) }
        } finally {
            AppPrefs.putConflictDefaultTop(context, key, previous)
        }
    }
}
