// 文件路径: feature/bangumi/ui/player/BangumiPlayerComponents.kt
package com.android.purebilibili.feature.bangumi.ui.player

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
// 🌈 Material Icons Extended - 亮度图标
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.ui.components.SponsorSkipButton
import com.android.purebilibili.feature.video.ui.components.SpeedSelectionMenu
import com.android.purebilibili.feature.video.ui.components.SpeedButton
import com.android.purebilibili.feature.video.ui.components.PlaybackSpeed
import com.android.purebilibili.feature.video.ui.components.DanmakuSettingsPanel
import com.android.purebilibili.data.model.response.SponsorSegment

/**
 * 手势模式枚举
 */
enum class BangumiGestureMode { None, Brightness, Volume, Seek }

/**
 * 增强版播放器视图
 * 支持：左侧亮度调节、右侧音量调节、进度拖动、弹幕显示、倍速、弹幕设置
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun BangumiPlayerView(
    exoPlayer: ExoPlayer,
    danmakuManager: DanmakuManager,
    danmakuEnabled: Boolean,
    onDanmakuToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    currentQuality: Int = 0,
    acceptQuality: List<Int> = emptyList(),
    acceptDescription: List<String> = emptyList(),
    onQualityChange: (Int) -> Unit = {},
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    sponsorSegment: SponsorSegment? = null,
    showSponsorSkipButton: Boolean = false,
    onSponsorSkip: () -> Unit = {},
    onSponsorDismiss: () -> Unit = {},
    //  新增：倍速控制
    currentSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {},
    //  新增：弹幕设置
    danmakuOpacity: Float = 0.85f,
    danmakuFontScale: Float = 1.0f,
    danmakuSpeed: Float = 1.0f,
    danmakuDisplayArea: Float = 0.5f,
    onDanmakuOpacityChange: (Float) -> Unit = {},
    onDanmakuFontScaleChange: (Float) -> Unit = {},
    onDanmakuSpeedChange: (Float) -> Unit = {},
    onDanmakuDisplayAreaChange: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    
    // 音频管理
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) }
    
    // 控制层状态
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }  //  倍速菜单
    var showDanmakuSettings by remember { mutableStateOf(false) }  //  弹幕设置面板
    
    // 手势状态
    var gestureMode by remember { mutableStateOf(BangumiGestureMode.None) }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var seekPreviewPosition by remember { mutableLongStateOf(0L) }
    
    // 亮度状态
    var currentBrightness by remember {
        mutableFloatStateOf(
            try {
                android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
            } catch (e: Exception) { 0.5f }
        )
    }
    
    // 播放器状态
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    
    // 监听播放器状态
    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            duration = exoPlayer.duration.coerceAtLeast(1L)
            currentPosition = exoPlayer.currentPosition
            if (gestureMode != BangumiGestureMode.Seek) {
                currentProgress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            kotlinx.coroutines.delay(200)
        }
    }
    
    // 自动隐藏控制
    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls && gestureMode == BangumiGestureMode.None) {
            kotlinx.coroutines.delay(4000)
            if (System.currentTimeMillis() - lastInteractionTime >= 4000) {
                showControls = false
            }
        }
    }
    
    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                        if (showControls) lastInteractionTime = System.currentTimeMillis()
                    },
                    onDoubleTap = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
                )
            }
            .then(
                Modifier.pointerInput(isFullscreen) {
                    val screenWidth = size.width.toFloat()
                    val screenHeight = size.height.toFloat()
                    
                    detectDragGestures(
                        onDragStart = { offset ->
                            showControls = true
                            lastInteractionTime = System.currentTimeMillis()
                            dragDelta = 0f
                            seekPreviewPosition = currentPosition
                            gestureMode = BangumiGestureMode.None
                        },
                        onDragEnd = {
                            if (gestureMode == BangumiGestureMode.Seek && kotlin.math.abs(dragDelta) > 20f) {
                                exoPlayer.seekTo(seekPreviewPosition)
                            }
                            gestureMode = BangumiGestureMode.None
                        },
                        onDragCancel = { gestureMode = BangumiGestureMode.None },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            
                            if (gestureMode == BangumiGestureMode.None) {
                                gestureMode = if (isFullscreen && kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                    BangumiGestureMode.Seek
                                } else if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) {
                                    if (change.position.x < screenWidth * 0.5f) {
                                        gestureValue = currentBrightness
                                        BangumiGestureMode.Brightness
                                    } else {
                                        gestureValue = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                                        BangumiGestureMode.Volume
                                    }
                                } else {
                                    BangumiGestureMode.None
                                }
                            }
                            
                            when (gestureMode) {
                                BangumiGestureMode.Brightness -> {
                                    gestureValue = (gestureValue - dragAmount.y / screenHeight).coerceIn(0f, 1f)
                                    currentBrightness = gestureValue
                                    (context as? Activity)?.window?.let { window ->
                                        val params = window.attributes
                                        params.screenBrightness = gestureValue
                                        window.attributes = params
                                    }
                                }
                                BangumiGestureMode.Volume -> {
                                    gestureValue = (gestureValue - dragAmount.y / screenHeight).coerceIn(0f, 1f)
                                    audioManager.setStreamVolume(
                                        android.media.AudioManager.STREAM_MUSIC,
                                        (gestureValue * maxVolume).toInt(),
                                        0
                                    )
                                }
                                BangumiGestureMode.Seek -> {
                                    dragDelta += dragAmount.x
                                    val seekDelta = (dragDelta / screenWidth * duration).toLong()
                                    seekPreviewPosition = (currentPosition + seekDelta).coerceIn(0L, duration)
                                    currentProgress = (seekPreviewPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                }
                                else -> {}
                            }
                        }
                    )
                }
            )
    ) {
        // PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 弹幕层 - 使用 DanmakuRenderEngine
        if (danmakuEnabled) {
            AndroidView(
                factory = { ctx ->
                    com.bytedance.danmaku.render.engine.DanmakuView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        android.util.Log.w("BangumiPlayer", "🎯 DanmakuView factory: creating new view")
                        danmakuManager.attachView(this)
                    }
                },
                update = { view ->
                    if (view.width > 0 && view.height > 0) {
                        android.util.Log.d("BangumiPlayer", " DanmakuView update: size=${view.width}x${view.height}")
                        danmakuManager.attachView(view)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 手势指示器（横屏：全部，竖屏：仅亮度和音量）
        val showGestureIndicator = gestureMode != BangumiGestureMode.None && 
            (isFullscreen || gestureMode == BangumiGestureMode.Brightness || gestureMode == BangumiGestureMode.Volume)
        if (showGestureIndicator) {
            BangumiGestureIndicator(
                mode = gestureMode,
                value = when (gestureMode) {
                    BangumiGestureMode.Brightness -> currentBrightness
                    BangumiGestureMode.Volume -> gestureValue
                    BangumiGestureMode.Seek -> currentProgress
                    else -> 0f
                },
                seekTime = if (gestureMode == BangumiGestureMode.Seek) seekPreviewPosition else null,
                duration = duration,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // 控制层
        androidx.compose.animation.AnimatedVisibility(
            visible = showControls && gestureMode == BangumiGestureMode.None,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 返回按钮
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(CupertinoIcons.Default.ChevronBackward, "返回", tint = Color.White)
                }
                
                // 顶部右侧按钮组
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 弹幕开关按钮
                    Surface(
                        onClick = onDanmakuToggle,
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (danmakuEnabled) CupertinoIcons.Default.TextBubble else CupertinoIcons.Default.TextBubble,
                                contentDescription = "弹幕",
                                tint = if (danmakuEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (danmakuEnabled) "弹幕" else "弹幕关",
                                color = if (danmakuEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    //  弹幕设置按钮（仅横屏显示）
                    if (isFullscreen && danmakuEnabled) {
                        Surface(
                            onClick = { showDanmakuSettings = true },
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(
                                CupertinoIcons.Default.Gear,
                                contentDescription = "弹幕设置",
                                tint = Color.White.copy(0.9f),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                    
                    //  倍速按钮
                    SpeedButton(
                        currentSpeed = currentSpeed,
                        onClick = { showSpeedMenu = true }
                    )
                    
                    // 画质选择按钮
                    if (acceptDescription.isNotEmpty()) {
                        val currentQualityLabel = acceptDescription.getOrNull(
                            acceptQuality.indexOf(currentQuality)
                        ) ?: "自动"
                        
                        Surface(
                            onClick = { showQualityMenu = true },
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = currentQualityLabel,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                
                // 播放/暂停按钮
                IconButton(
                    onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                ) {
                    Icon(
                        if (isPlaying) CupertinoIcons.Default.Pause else CupertinoIcons.Default.Play,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // 底部控制栏
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // 进度条（全屏和竖屏都显示）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            FormatUtils.formatDuration((currentPosition / 1000).toInt()), 
                            color = Color.White, 
                            fontSize = 12.sp
                        )
                        
                        Slider(
                            value = currentProgress,
                            onValueChange = { 
                                lastInteractionTime = System.currentTimeMillis()
                                currentProgress = it 
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo((currentProgress * duration).toLong())
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        
                        Text(
                            FormatUtils.formatDuration((duration / 1000).toInt()), 
                            color = Color.White, 
                            fontSize = 12.sp
                        )
                        
                        // 全屏/退出全屏按钮
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isFullscreen) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight, 
                                "全屏", 
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // 画质选择菜单
        if (showQualityMenu && acceptDescription.isNotEmpty()) {
            BangumiQualityMenu(
                qualities = acceptDescription,
                qualityIds = acceptQuality,
                currentQualityId = currentQuality,
                onQualitySelected = { qn ->
                    onQualityChange(qn)
                    showQualityMenu = false
                },
                onDismiss = { showQualityMenu = false }
            )
        }
        
        //  倍速选择菜单
        if (showSpeedMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSpeedMenu = false },
                contentAlignment = Alignment.Center
            ) {
                SpeedSelectionMenu(
                    currentSpeed = currentSpeed,
                    onSpeedSelected = { speed ->
                        onSpeedChange(speed)
                        exoPlayer.setPlaybackSpeed(speed)  //  直接应用倍速
                        showSpeedMenu = false
                    },
                    onDismiss = { showSpeedMenu = false }
                )
            }
        }
        
        //  弹幕设置面板
        if (showDanmakuSettings) {
            DanmakuSettingsPanel(
                opacity = danmakuOpacity,
                fontScale = danmakuFontScale,
                speed = danmakuSpeed,
                displayArea = danmakuDisplayArea,
                onOpacityChange = onDanmakuOpacityChange,
                onFontScaleChange = onDanmakuFontScaleChange,
                onSpeedChange = onDanmakuSpeedChange,
                onDisplayAreaChange = onDanmakuDisplayAreaChange,
                onDismiss = { showDanmakuSettings = false }
            )
        }
        
        // 空降助手跳过按钮 (位置调整到进度条上方)
        SponsorSkipButton(
            segment = sponsorSegment,
            visible = showSponsorSkipButton,
            onSkip = onSponsorSkip,
            onDismiss = onSponsorDismiss,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 16.dp)  //  向上偏移避免与进度条重叠
        )
    }
}

/**
 * 手势指示器
 */
@Composable
fun BangumiGestureIndicator(
    mode: BangumiGestureMode,
    value: Float,
    seekTime: Long?,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            when (mode) {
                BangumiGestureMode.Brightness -> {
                    //  亮度图标：CupertinoIcons SunMax (iOS SF Symbols 风格)
                    Icon(CupertinoIcons.Default.SunMax, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("亮度", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                BangumiGestureMode.Volume -> {
                    //  动态音量图标：3 级
                    val volumeIcon = when {
                        value < 0.01f -> CupertinoIcons.Default.SpeakerSlash
                        value < 0.5f -> CupertinoIcons.Default.Speaker
                        else -> CupertinoIcons.Default.SpeakerWave2
                    }
                    Icon(volumeIcon, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("音量", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                BangumiGestureMode.Seek -> {
                    Text(
                        "${FormatUtils.formatDuration(((seekTime ?: 0) / 1000).toInt())} / ${FormatUtils.formatDuration((duration / 1000).toInt())}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {}
            }
        }
    }
}

/**
 * 可拖动的迷你进度条（竖屏模式） - 紧凑样式
 */
@Composable
fun BangumiMiniProgressBar(
    player: ExoPlayer,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var bufferedProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    
    // 定期更新进度
    LaunchedEffect(player) {
        while (true) {
            if (player.duration > 0 && !isDragging) {
                progress = player.currentPosition.toFloat() / player.duration
                bufferedProgress = player.bufferedPosition.toFloat() / player.duration
            }
            kotlinx.coroutines.delay(200)
        }
    }
    
    Box(
        modifier = modifier
            .height(12.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    val seekPosition = (fraction * player.duration).toLong()
                    player.seekTo(seekPosition)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        val seekPosition = (dragProgress * player.duration).toLong()
                        player.seekTo(seekPosition)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { _, dragAmount ->
                        dragProgress = (dragProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 进度条容器 - 实际显示的细条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.DarkGray.copy(alpha = 0.5f))
        ) {
            // 缓冲进度
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                    .background(Color.White.copy(alpha = 0.3f))
            )
            // 播放进度
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((if (isDragging) dragProgress else progress).coerceIn(0f, 1f))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * 番剧画质选择菜单
 */
@Composable
fun BangumiQualityMenu(
    qualities: List<String>,
    qualityIds: List<Int>,
    currentQualityId: Int,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    fun getQualityTag(qn: Int): String? {
        return when (qn) {
            127, 126, 125, 120, 116, 112 -> "大会员"
            else -> null
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = false) {},
            color = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "画质选择",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color.White.copy(0.1f))
                
                qualities.forEachIndexed { index, quality ->
                    val qn = qualityIds.getOrNull(index) ?: 0
                    val isSelected = qn == currentQualityId
                    val tag = getQualityTag(qn)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(qn) }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(0.9f),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        
                        if (tag != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFFB7299),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (isSelected) {
                            Icon(
                                CupertinoIcons.Default.Checkmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
