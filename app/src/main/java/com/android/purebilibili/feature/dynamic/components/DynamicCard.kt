// 文件路径: feature/dynamic/components/DynamicCard.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
//  已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicType

/**
 *  动态卡片V2 - 官方风格
 */
@Composable
fun DynamicCardV2(
    item: DynamicItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit,
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> },
    gifImageLoader: ImageLoader,
    //  [新增] 评论/转发/点赞回调
    onCommentClick: (dynamicId: String) -> Unit = {},
    onRepostClick: (dynamicId: String) -> Unit = {},
    onLikeClick: (dynamicId: String) -> Unit = {},
    isLiked: Boolean = false
) {
    val author = item.modules.module_author
    val content = item.modules.module_dynamic
    val stat = item.modules.module_stat
    val type = DynamicType.fromApiValue(item.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        //  [新增] 更多菜单状态
        var showMoreMenu by remember { mutableStateOf(false) }
        val context = LocalContext.current
        
        //  用户头部（头像 + 名称 + 时间 + 更多）
        if (author != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(author.face.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = author.mid > 0) { onUserClick(author.mid) },
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        author.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (author.vip?.status == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        author.pub_time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }
                
                //  [修复] 更多按钮 + 下拉菜单
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            CupertinoIcons.Default.Ellipsis,
                            contentDescription = "更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    }
                    
                    // 下拉菜单 - 使用白色背景
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(androidx.compose.ui.graphics.Color.White)
                    ) {
                        // 复制链接
                        DropdownMenuItem(
                            text = { Text("复制链接", color = androidx.compose.ui.graphics.Color.Black) },
                            leadingIcon = { 
                                Icon(
                                    CupertinoIcons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                ) 
                            },
                            onClick = {
                                showMoreMenu = false
                                // 复制动态链接到剪贴板
                                val dynamicUrl = "https://t.bilibili.com/${item.id_str}"
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("动态链接", dynamicUrl))
                                android.widget.Toast.makeText(context, "已复制链接", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        
                        // 不感兴趣
                        DropdownMenuItem(
                            text = { Text("不感兴趣", color = androidx.compose.ui.graphics.Color.Black) },
                            leadingIcon = { 
                                Icon(
                                    CupertinoIcons.Default.EyeSlash,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = androidx.compose.ui.graphics.Color.Black
                                ) 
                            },
                            onClick = {
                                showMoreMenu = false
                                android.widget.Toast.makeText(context, "已标记为不感兴趣", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        //  动态内容文字（支持@高亮）
        content?.desc?.let { desc ->
            if (desc.text.isNotEmpty()) {
                RichTextContent(
                    desc = desc,
                    onUserClick = onUserClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        //  视频类型动态 - 大图预览
        content?.major?.archive?.let { archive ->
            VideoCardLarge(
                archive = archive,
                onClick = { onVideoClick(archive.bvid) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        //  图片类型动态（支持GIF + 点击预览）
        content?.major?.draw?.let { draw ->
            var selectedImageIndex by remember { mutableIntStateOf(-1) }
            
            DrawGridV2(
                items = draw.items,
                gifImageLoader = gifImageLoader,
                onImageClick = { index -> selectedImageIndex = index }
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // 全屏图片预览
            if (selectedImageIndex >= 0) {
                ImagePreviewDialog(
                    images = draw.items.map { it.src },
                    initialIndex = selectedImageIndex,
                    onDismiss = { selectedImageIndex = -1 }
                )
            }
        }
        
        //  直播推荐动态
        content?.major?.live_rcmd?.let { liveRcmd ->
            LiveCard(
                liveRcmd = liveRcmd,
                onLiveClick = onLiveClick
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        //  转发动态 - 嵌套显示原始内容
        if (type == DynamicType.FORWARD && item.orig != null) {
            ForwardedContent(
                orig = item.orig,
                onVideoClick = onVideoClick,
                onUserClick = onUserClick,
                gifImageLoader = gifImageLoader
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        //  [新增] 底部操作栏：转发、评论、点赞
        stat?.let { statModule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 转发按钮
                ActionButton(
                    icon = io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Default.ArrowTurnUpRight,
                    count = statModule.forward.count,
                    label = "转发",
                    onClick = { onRepostClick(item.id_str) }
                )
                
                // 评论按钮
                ActionButton(
                    icon = io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Default.Message,
                    count = statModule.comment.count,
                    label = "评论",
                    onClick = { onCommentClick(item.id_str) }
                )
                
                // 点赞按钮
                ActionButton(
                    icon = if (isLiked) io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Filled.Heart 
                           else io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Default.Heart,
                    count = statModule.like.count,
                    label = "点赞",
                    isActive = isLiked,
                    onClick = { onLikeClick(item.id_str) }
                )
            }
        }
    }
}

/**
 *  富文本内容（支持@提及高亮）
 */
@Composable
fun RichTextContent(
    desc: DynamicDesc,
    onUserClick: (Long) -> Unit
) {
    // 简化版：直接渲染文本，@提及用蓝色
    val text = buildAnnotatedString {
        val rawText = desc.text
        var lastEnd = 0
        
        // 查找 @xxx 模式
        val atPattern = Regex("@[^@\\s]+")
        atPattern.findAll(rawText).forEach { match ->
            // 普通文本
            if (match.range.first > lastEnd) {
                append(rawText.substring(lastEnd, match.range.first))
            }
            // @提及
            withStyle(SpanStyle(color = iOSBlue, fontWeight = FontWeight.Medium)) {
                append(match.value)
            }
            lastEnd = match.range.last + 1
        }
        // 剩余文本
        if (lastEnd < rawText.length) {
            append(rawText.substring(lastEnd))
        }
    }
    
    Text(
        text = text,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/**
 *  紧凑列表卡片 - 单行显示
 */
@Composable
fun DynamicCardCompact(
    item: DynamicItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit
) {
    val author = item.modules.module_author
    val content = item.modules.module_dynamic
    val stat = item.modules.module_stat
    
    // 获取内容预览文本
    val previewText = content?.desc?.text?.take(50) 
        ?: content?.major?.archive?.title 
        ?: "动态"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 如果有视频则跳转视频
                content?.major?.archive?.let { onVideoClick(it.bvid) }
                    ?: author?.let { onUserClick(it.mid) }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),  //  优化间距
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        if (author != null) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(author.face.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = author.mid > 0) { onUserClick(author.mid) },
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        // 内容区
        Column(modifier = Modifier.weight(1f)) {
            // 用户名 + 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    author?.name ?: "",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    author?.pub_time ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 内容预览
            Text(
                previewText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        // 封面缩略图（如果有视频）
        content?.major?.archive?.let { archive ->
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(archive.cover.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
