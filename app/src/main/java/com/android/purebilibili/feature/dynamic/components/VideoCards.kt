// 文件路径: feature/dynamic/components/VideoCards.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.ArchiveMajor

/**
 *  大尺寸视频卡片
 *  🎨 [优化] 更大圆角、渐变遮罩、更好的信息展示
 */
@Composable
fun VideoCardLarge(
    archive: ArchiveMajor,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = remember(archive.cover) {
        val raw = archive.cover.trim()
        when {
            raw.startsWith("https://") -> raw
            raw.startsWith("http://") -> raw.replace("http://", "https://")
            raw.startsWith("//") -> "https:$raw"
            raw.isNotEmpty() -> "https://$raw"
            else -> ""
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))  //  [优化] 更大圆角 8dp → 12dp
            .clickable(onClick = onClick)
    ) {
        // 视频封面 - 16:9
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))  //  [优化] 封面也使用 12dp 圆角
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(coverUrl)
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            //  [新增] 底部渐变遮罩 - 增加信息可读性
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // 时长标签 - 右下角
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp))  //  [优化] 更大圆角
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(archive.duration_text, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
            
            // 播放量和弹幕 - 左下角
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(CupertinoIcons.Default.Play, null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(3.dp))
                Text(archive.stat.play, fontSize = 12.sp, color = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(CupertinoIcons.Default.Message, null, modifier = Modifier.size(13.dp), tint = Color.White)  //  弹幕图标
                Spacer(modifier = Modifier.width(3.dp))
                Text(archive.stat.danmaku, fontSize = 12.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))  //  [优化] 增加间距
        
        // 视频标题
        Text(
            archive.title,
            fontSize = 15.sp,  //  [优化] 稍大字体
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,  //  [优化] 行高
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 *  小尺寸视频卡片（用于转发）
 */
@Composable
fun VideoCardSmall(
    archive: ArchiveMajor,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = remember(archive.cover) {
        val raw = archive.cover.trim()
        when {
            raw.startsWith("https://") -> raw
            raw.startsWith("http://") -> raw.replace("http://", "https://")
            raw.startsWith("//") -> "https:$raw"
            raw.isNotEmpty() -> "https://$raw"
            else -> ""
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面
        Box(
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
        ) {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(coverUrl)
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(archive.duration_text, fontSize = 10.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 标题
        Text(
            archive.title,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
