package com.easykebiao.app.data.memo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * 备忘里的图片（0.15.0）。
 *
 * 为什么不用新表：一条备忘的正文本来就是一段自由文本，图片在正文里的位置也是「跟着文字走」的。
 * 把图片名内嵌在正文里（`[[img:<文件名>]]`）有两个好处：
 *  · 不需要数据库迁移，旧版本读到带标记的正文最多是多一行乱码，不会崩；
 *  · 导出/备份只要抄 content 一列，图片和它的位置一起带走。
 * 编辑器里看到的是「纯文本 + 缩略图条」，标记不出现在文本框里（[split] / [join] 负责来回转换）。
 */
object MemoImages {

    private const val DIR = "memo_images"
    private const val MAX_EDGE = 1600
    private const val QUALITY = 85
    private val MARKER = Regex("""\[\[img:([A-Za-z0-9._-]+)]]""")

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    /** 把用户选的图片拷进应用私有目录，返回落盘文件名；失败返回 null。 */
    fun store(context: Context, uri: Uri): String? = runCatching {
        val name = "${UUID.randomUUID()}.jpg"
        val target = file(context, name)
        val resolver = context.contentResolver
        // 先只看尺寸，决定缩到多少：手机原图动辄 4K，原样存会把私有目录撑爆
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        target.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        }
        bitmap.recycle()
        name
    }.getOrNull()

    fun delete(context: Context, name: String) {
        runCatching { file(context, name).takeIf { it.exists() }?.delete() }
    }

    /** 正文里出现过的全部图片文件名（按出现顺序）。 */
    fun imagesOf(content: String): List<String> =
        MARKER.findAll(content).map { it.groupValues[1] }.toList()

    /** 去掉图片标记后的纯文本 —— 列表预览用（标记不能漏到卡片上）。 */
    fun strip(content: String): String =
        MARKER.replace(content, "")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .trim()
            .replace(Regex("\n{3,}"), "\n\n")

    /** 正文 → (纯文本, 图片列表)。编辑弹窗打开时用。 */
    fun split(content: String): Pair<String, List<String>> = strip(content) to imagesOf(content)

    /** (纯文本, 图片列表) → 正文。保存时用；没有图片就原样返回文本，不留空行。 */
    fun join(text: String, images: List<String>): String {
        val body = text.trim()
        if (images.isEmpty()) return body
        val marks = images.joinToString("\n") { "[[img:$it]]" }
        return if (body.isEmpty()) marks else "$body\n$marks"
    }

    fun exists(context: Context, name: String): Boolean = file(context, name).exists()
}
