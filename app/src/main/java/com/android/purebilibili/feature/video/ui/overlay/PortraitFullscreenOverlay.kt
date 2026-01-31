package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Share
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils

/**
 * 竖屏全屏覆盖层 (TikTok 风格)
 *
 * 包含：
 * - 顶部栏：返回按钮 + 清屏按钮
 * - 右侧栏：点赞/投币/收藏/评论/转发
 * - 底部栏：UP主信息 + 标题 + 进度条
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortraitFullscreenOverlay(
    title: String,
    authorName: String = "",
    authorFace: String = "",
    isPlaying: Boolean,
    progress: PlayerProgress,
    
    // 互动状态
    isLiked: Boolean,
    isCoined: Boolean,
    isFavorited: Boolean,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    
    // 控制状态
    currentSpeed: Float,
    currentQualityLabel: String,
    currentRatio: VideoAspectRatio,
    danmakuEnabled: Boolean,
    isStatusBarHidden: Boolean,
    
    // 回调
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    onSpeedClick: () -> Unit,
    onQualityClick: () -> Unit,
    onRatioClick: () -> Unit,
    onDanmakuToggle: () -> Unit,
    onDanmakuInputClick: () -> Unit,
    onToggleStatusBar: () -> Unit, // 暂时保留，但 UI 上可能集成到清屏
    
    modifier: Modifier = Modifier
) {
    // 控制层显示状态
    var showControls by remember { mutableStateOf(true) }
    
    @OptIn(ExperimentalFoundationApi::class)
    Box(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { 
                    // 点击屏幕切换显示/隐藏控件
                    showControls = !showControls
                },
                onDoubleClick = { onPlayPause() }
            )
    ) {
        
        // 控件层
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // 1. 顶部栏 (返回 + 清屏)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 返回按钮
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.ChevronBackward,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                }
                
                // 清屏按钮 (右上角)
                IconButton(
                    onClick = { showControls = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Default.EyeSlash, // 或其他清屏图标
                        contentDescription = "清屏",
                        tint = Color.White
                    )
                }

                // 2. 右侧互动栏
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp, bottom = 100.dp), // 留出底部空间
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头像 (如果有)
                    if (authorFace.isNotEmpty()) {
                        Box(contentAlignment = Alignment.BottomCenter) {
                            Surface(
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                modifier = Modifier.size(48.dp)
                            ) {
                                AsyncImage(
                                    model = FormatUtils.fixImageUrl(authorFace),
                                    contentDescription = authorName,
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // 关注按钮 (小加号)
                            Surface(
                                shape = CircleShape,
                                color = BiliPink,
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(y = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "关注",
                                    tint = Color.White,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    InteractionButton(
                        icon = if (isLiked) CupertinoIcons.Filled.Heart else CupertinoIcons.Default.Heart,
                        label = "点赞",
                        isActive = isLiked,
                        activeColor = BiliPink,
                        onClick = onLikeClick
                    )
                    
                    InteractionButton(
                        icon = com.android.purebilibili.core.ui.AppIcons.BiliCoin, 
                        label = "投币",
                        isActive = isCoined,
                        activeColor = BiliPink,
                        onClick = onCoinClick
                    )
                    
                    InteractionButton(
                        icon = if (isFavorited) CupertinoIcons.Filled.Star else CupertinoIcons.Outlined.Star,
                        label = "收藏",
                        isActive = isFavorited,
                        activeColor = BiliPink,
                        onClick = onFavoriteClick
                    )
                    
                    InteractionButton(
                        icon = CupertinoIcons.Default.BubbleLeft, // 评论
                        label = "评论",
                        isActive = false,
                        onClick = onCommentClick
                    )
                    
                    InteractionButton(
                        icon = Icons.Rounded.Share, // 分享
                        label = "分享",
                        isActive = false,
                        onClick = onShareClick
                    )
                }
                
                // 3. 底部信息栏
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(start = 16.dp, end = 80.dp, bottom = 12.dp, top = 60.dp) // End padding to avoid overlap with sidebar
                ) {
                    // UP主名字
                    Text(
                        text = "@$authorName",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 标题
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        maxLines = 2,
                        lineHeight = 20.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 简易进度条
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 进度条 slider
                        Slider(
                            value = if (progress.duration > 0) progress.current.toFloat() / progress.duration else 0f,
                            onValueChange = { value ->
                                val target = (value * progress.duration).toLong()
                                onSeekStart() // 暂存播放状态
                                onSeek(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White.copy(alpha = 0.8f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp),
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    colors = SliderDefaults.colors(thumbColor = Color.White),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 时间文本
                        Text(
                            text = com.android.purebilibili.core.util.FormatUtils.formatDuration(progress.current) + 
                                   " / " + 
                                   com.android.purebilibili.core.util.FormatUtils.formatDuration(progress.duration),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 互动按钮组件 (TikTok 风格：带文字，右侧竖排)
 */
@Composable
private fun InteractionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color = BiliPink,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        ) { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
