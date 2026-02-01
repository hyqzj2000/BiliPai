package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
 * 竖屏全屏覆盖层 (TikTok 风格) - 重构版
 *
 * 包含：
 * - 顶部栏：返回按钮 + 清屏按钮
 * - 右侧栏：点赞/投币/收藏/评论/转发 (调整边距，避免重叠)
 * - 底部栏：UP主信息 + 标题 + 细条进度条
 * - 手势处理：移除根点击拦截，允许下层手势或外部手势控制器工作
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
    
    // 显示状态 (由外部控制，或内部状态)
    showControls: Boolean = true,
    
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
    onToggleStatusBar: () -> Unit,
    
    modifier: Modifier = Modifier
) {
    // 布局常量
    val rightSidebarWidth = 60.dp
    val bottomInfoHeight = 160.dp // 为标题和说明留出空间
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        
        // 控件层动画
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
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.ChevronBackward,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    
                    // 右上角功能区
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 更多功能按钮... 可以添加
                         IconButton(
                            onClick = onToggleStatusBar, // 这里暂时用作清屏/沉浸式切换
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if(isStatusBarHidden) CupertinoIcons.Default.Eye else CupertinoIcons.Default.EyeSlash,
                                contentDescription = "清屏",
                                tint = Color.White
                            )
                        }
                    }
                }

                // 2. 右侧互动栏
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 120.dp), // 这里的 bottom padding 要避开底部进度条区域
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
                                    .size(18.dp)
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
                        Spacer(modifier = Modifier.height(6.dp))
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
                
                // 3. 底部信息栏 (UP主、标题)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(bottom = 0.dp) // 紧贴底部，进度条在最下方
                ) {
                    // 渐变背景，保证文字可读性
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                            .padding(start = 12.dp, end = 80.dp, bottom = 24.dp, top = 40.dp) // 右侧留出空间给按钮
                    ) {
                        Column {
                            // UP主名字
                            Text(
                                text = "@$authorName",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // 标题
                            Text(
                                text = title,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 15.sp,
                                maxLines = 3,
                                lineHeight = 22.sp,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        
        // 4. 底部进度条 (Fixed at bottom, visible even when controls hidden/dimmed slightly)
        // 使用自定义细条进度条
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 8.dp, end = 8.dp) // 底部留一点边距
                .height(30.dp) // 触摸热区高度
            ,
            contentAlignment = Alignment.Center
        ) {
             ThinWigglyProgressBar(
                progress = if (progress.duration > 0) progress.current.toFloat() / progress.duration else 0f,
                onSeek = { fraction ->
                     val target = (fraction * progress.duration).toLong()
                     onSeek(target)
                },
                onSeekStart = onSeekStart,
                duration = progress.duration, // 传递时长用于显示
                bufferProgress = 0f // 暂无缓冲进度
            )
        }
    }
}

/**
 * 抖音风格细条进度条
 * - 平时：细条 (2dp)
 * - 拖拽中：变粗 (8dp) + 显示当前时间
 */
@Composable
fun ThinWigglyProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekStart: () -> Unit,
    duration: Long,
    bufferProgress: Float = 0f
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    
    // 显示的进度：如果正在拖拽，显示拖拽值，否则显示真实进度
    val displayProgress = if (isDragging) dragProgress else progress
    
    // 动画状态
    val barHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragging) 12.dp else 2.dp,
        label = "barHeight"
    )
    
    val thumbSize by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp, // 拖拽时显示滑块，平时隐藏
        label = "thumbSize"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onSeekStart()
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek(dragProgress)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newProgress = (dragProgress + dragAmount / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                    }
                )
            }
            // 也支持点击跳转
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true // 按下变成拖拽态
                        onSeekStart()
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        tryAwaitRelease()
                        isDragging = false
                        onSeek(dragProgress)
                    }
                ) 
            }
        ,
        contentAlignment = Alignment.CenterStart
    ) {
        // 背景轨道
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        )
        
        // 进度 (当前进度)
        Box(
            modifier = Modifier
                .fillMaxWidth(displayProgress)
                .height(barHeight)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
        )
        
        // 滑块 (Thumb) - 仅拖拽时显示
        if (isDragging) {
             Box(
                modifier = Modifier
                    .offset(x = with(LocalDensity.current) { 
                        // 计算滑块位置：(maxWidth * progress) - (thumbSize / 2)
                        // 需要获取 Box 宽度，这里简化处理，使用 Alignment 配合 padding ?? 
                        // 由于 Box 是 fillMaxWidth，我们很难直接拿到 px 宽度在 composition 阶段。
                        // 使用 fillMaxWidth(progress) 的右边缘作为锚点可能更简单，但 Box 嵌套 Box 会导致尺寸依赖。
                        // 简单的做法：把滑块放在一个 Row 里或者使用 Spacer weight
                        // 修正：使用 Spacer 挤压
                         0.dp // 暂时置零，下面用 Row 实现对齐
                    })
            )
        }
        
        // 使用 Box + BiasAlignment 来定位滑块，避免 weight 为 0 导致的崩溃
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
             val bias = (displayProgress * 2f) - 1f
             Box(
                 modifier = Modifier
                     .align(androidx.compose.ui.Alignment.Center) // 使用父容器中心作为基准
                     .offset(x = 0.dp) // 需要根据 bias 调整
             )
             
             // 更简单的做法：直接使用 Alignment(horizontalBias, 0f)
             Box(
                 modifier = Modifier
                     .size(thumbSize)
                     .align(androidx.compose.ui.BiasAlignment(bias, 0f))
                     .background(Color.White, CircleShape)
             )
        }
        
        // 拖拽时的气泡提示 (上方)
        if (isDragging) {
             // 计算时间文本
             val totalSeconds = duration / 1000
             val currentSeconds = (totalSeconds * displayProgress).toLong()
             val timeText = FormatUtils.formatDuration(currentSeconds) + " / " + FormatUtils.formatDuration(totalSeconds)
             
             Box(
                 modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp) // 向上偏移
             ) {
                 Text(
                     text = timeText,
                     color = Color.White,
                     fontSize = 18.sp,
                     fontWeight = FontWeight.Bold,
                     style = MaterialTheme.typography.titleLarge,
                     modifier = Modifier
                         .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                         .padding(horizontal = 12.dp, vertical = 6.dp)
                 )
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
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else Color.White,
            modifier = Modifier.size(36.dp) // 稍微大一点
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black,
                    blurRadius = 4f
                )
            )
        )
    }
}
