package com.android.purebilibili.feature.video.ui.overlay

internal enum class FullscreenDoubleTapAction {
    SeekBackward,
    TogglePlayPause,
    SeekForward
}

internal fun resolveFullscreenDoubleTapAction(
    relativeX: Float,
    doubleTapSeekEnabled: Boolean,
    playWhenReady: Boolean = true
): FullscreenDoubleTapAction {
    if (!doubleTapSeekEnabled) return FullscreenDoubleTapAction.TogglePlayPause
    if (!playWhenReady) return FullscreenDoubleTapAction.TogglePlayPause

    return when {
        relativeX < 0.3f -> FullscreenDoubleTapAction.SeekBackward
        relativeX > 0.7f -> FullscreenDoubleTapAction.SeekForward
        else -> FullscreenDoubleTapAction.TogglePlayPause
    }
}
