package com.easykebiao.app.data.memo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 备忘图片标记的往返（0.15.0）。
 *
 * 这里只测纯文本层面的 split/join/strip —— 不碰 Bitmap 与文件系统，
 * 图片落盘那部分靠真机跑一遍验证（模拟器里插图→保存→重开）。
 */
class MemoImagesTest {

    @Test
    fun strip_removes_markers_and_keeps_text() {
        val content = "周五交实验报告\n[[img:a.jpg]]\n11月27日"
        // 标记独占一行时，去掉它会留下一个空行：这里容忍空行，只要求文字都在、顺序不变
        assertEquals("周五交实验报告\n11月27日", MemoImages.strip(content).replace("\n\n", "\n"))
    }

    @Test
    fun imagesOf_returns_file_names_in_order() {
        val content = "x\n[[img:one.jpg]]\ny\n[[img:two-2.jpg]]"
        assertEquals(listOf("one.jpg", "two-2.jpg"), MemoImages.imagesOf(content))
    }

    @Test
    fun text_without_images_is_untouched() {
        val content = "只有文字，[[]] 这种不算标记"
        assertEquals(emptyList<String>(), MemoImages.imagesOf(content))
        assertEquals(content, MemoImages.strip(content))
    }

    @Test
    fun join_and_split_round_trip() {
        val text = "笔记正文"
        val images = listOf("a.jpg", "b.jpg")
        val joined = MemoImages.join(text, images)
        assertTrue(joined.startsWith(text))
        val (outText, outImages) = MemoImages.split(joined)
        assertEquals(text, outText)
        assertEquals(images, outImages)
    }

    @Test
    fun join_without_images_does_not_add_blank_lines() {
        assertEquals("只有文字", MemoImages.join("  只有文字 \n", emptyList()))
    }

    @Test
    fun join_with_only_images_returns_markers() {
        val joined = MemoImages.join("", listOf("x.jpg"))
        assertEquals("[[img:x.jpg]]", joined)
        assertEquals(listOf("x.jpg"), MemoImages.imagesOf(joined))
    }

    @Test
    fun marker_with_illegal_characters_is_not_treated_as_image() {
        // 路径穿越之类的写法不能被当成文件名
        val content = "[[img:../../secret.jpg]]"
        assertEquals(emptyList<String>(), MemoImages.imagesOf(content))
    }
}
