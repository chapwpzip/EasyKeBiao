package com.easykebiao.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.easykebiao.app.data.memo.MemoImages
import com.easykebiao.app.ui.theme.noRippleClickable

/**
 * 备忘缩略图（0.15.0）。
 *
 * 图片本体存在应用私有目录（见 [MemoImages]），这里只按文件名加载；
 * 文件被清理掉时 Coil 会给出空结果，界面留一个淡色底块而不是崩掉。
 */
@Composable
fun MemoThumb(
    name: String,
    size: Dp = 56.dp,
    radius: Dp = 10.dp,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Box(modifier = Modifier.size(size)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(MemoImages.file(context, name))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(radius))
                .background(Color.Black.copy(alpha = 0.06f))
        )
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .noRippleClickable(onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/** 一排缩略图（列表卡片用，最多 [limit] 张，超出显示 +N）。 */
@Composable
fun MemoThumbRow(
    names: List<String>,
    size: Dp = 56.dp,
    limit: Int = 3
) {
    if (names.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.take(limit).forEach { MemoThumb(name = it, size = size) }
        if (names.size > limit) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                com.nevoit.glasense.core.component.Text(
                    text = "+${names.size - limit}",
                    style = com.nevoit.glasense.theme.GlasenseTheme.type.footnoteEmphasized,
                    color = com.nevoit.glasense.theme.GlasenseTheme.colors.contentVariant
                )
            }
        }
    }
}
