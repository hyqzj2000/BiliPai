package com.android.purebilibili.core.util

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *  iOS 风格 Spring 动画预设
 * 
 * 基于 iOS Human Interface Guidelines 的动画参数，
 * 提供统一的弹性动画效果，让交互更加自然流畅。
 */
object iOSSpringSpecs {
    
    /**
     * 按钮点击反馈动画
     * - 快速响应，轻微回弹
     * - 适用于 IconButton、ActionButton 等
     */
    val ButtonPress: SpringSpec<Float> = SpringSpec(
        dampingRatio = 0.6f,
        stiffness = 400f
    )
    
    /**
     * 页面切换动画
     * - 无回弹，自然停止
     * - 适用于导航过渡、页面滑入滑出
     */
    val PageTransition: SpringSpec<Float> = SpringSpec(
        dampingRatio = 1f,  // 临界阻尼，无回弹
        stiffness = Spring.StiffnessMediumLow
    )
    
    /**
     * 卡片展开动画
     * - 适度回弹，有活力感
     * - 适用于卡片详情展开、BottomSheet 弹出
     */
    val CardExpand: SpringSpec<Float> = SpringSpec(
        dampingRatio = 0.8f,
        stiffness = 300f
    )
    
    /**
     * 侧边栏/抽屉动画
     * - 轻微回弹，快速响应
     * - 适用于 Sidebar、Drawer 展开收起
     */
    val Drawer: SpringSpec<Float> = SpringSpec(
        dampingRatio = 0.7f,
        stiffness = 350f
    )
    
    /**
     * 列表项入场动画
     * - 中等回弹，Q弹效果
     * - 适用于 LazyColumn 卡片入场
     */
    val ListItem: SpringSpec<Float> = SpringSpec(
        dampingRatio = 0.65f,
        stiffness = 300f
    )
    
    /**
     * 刷新指示器动画
     * - 轻微回弹
     * - 适用于下拉刷新旋转动画
     */
    val RefreshIndicator: SpringSpec<Float> = SpringSpec(
        dampingRatio = 0.7f,
        stiffness = 300f
    )
    
    /**
     * 缩放动画通用参数
     * - 适用于 scale 变换的通用预设
     */
    val Scale: SpringSpec<Float> = SpringSpec(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 *  列表项进场动画 (Premium 非线性动画)
 * 
 * 特点：
 * - 交错延迟实现波浪效果
 * - 从下方滑入 + 缩放 + 淡入
 * - 非线性缓动曲线 (FastOutSlowIn)
 * - Q弹果冻回弹效果
 * 
 * @param index: 列表项的索引，用于计算延迟时间
 * @param key: 用于触发重置动画的键值 (通常传视频ID)
 * @param initialOffsetY: 初始 Y 偏移量
 * @param animationEnabled: 是否启用动画 (设置开关)
 */
fun Modifier.animateEnter(
    index: Int = 0,
    key: Any? = Unit,
    initialOffsetY: Float = 80f,
    animationEnabled: Boolean = true
): Modifier = composed {
    //  如果动画被禁用，直接返回无动画效果
    if (!animationEnabled) {
        return@composed this
    }
    
    // 动画状态 - 始终初始化为需要动画的状态
    val alpha = remember(key) { Animatable(0f) }
    val translationY = remember(key) { Animatable(initialOffsetY) }
    val scale = remember(key) { Animatable(0.85f) }

    LaunchedEffect(key) {
        //  在 LaunchedEffect 内部检查，确保每次执行时都检查最新状态
        if (CardPositionManager.isReturningFromDetail) {
            //  直接设置为最终值，不播放动画
            alpha.snapTo(1f)
            translationY.snapTo(0f)
            scale.snapTo(1f)
            // 延迟清除标记，确保所有卡片都读取到
            delay(100)
            CardPositionManager.clearReturning()
            return@LaunchedEffect
        }
        
        //  交错延迟：每个卡片延迟 40ms，最多 300ms
        val delayMs = (index * 40L).coerceAtMost(300L)
        delay(delayMs)

        //  并行启动动画
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing // 非线性缓动
                )
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,    // 轻微过冲
                    stiffness = 300f         // 适中的弹性
                )
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.7f,     // 轻微过冲
                    stiffness = 350f         // 稍快的回弹
                )
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
        this.scaleX = scale.value
        this.scaleY = scale.value
    }
}

/**
 * 2. Q弹点击效果 (按压缩放)
 */
fun Modifier.bouncyClickable(
    scaleDown: Float = 0.90f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}