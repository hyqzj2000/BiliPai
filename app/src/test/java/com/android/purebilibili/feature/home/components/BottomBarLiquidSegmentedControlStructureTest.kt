package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarLiquidSegmentedControlStructureTest {

    @Test
    fun `segmented control keeps sliding glass by default with opt out flag`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt"
        )

        assertTrue(source.contains("BottomBarMotionProfile.ANDROID_NATIVE_FLOATING"))
        assertTrue(source.contains("resolveBottomBarRefractionMotionProfile("))
        assertTrue(source.contains(".background(containerColor, containerShape)"))
        assertTrue(source.contains("val neutralIndicatorColor = if (isDarkTheme) Color.White.copy(0.1f) else Color.Black.copy(0.1f)"))
        assertTrue(source.contains("resolveLiquidSegmentedIndicatorColor("))
        assertTrue(source.contains("liquidGlassEffectsEnabled: Boolean = true"))
        assertTrue(source.contains("dragSelectionEnabled: Boolean = true"))
        assertTrue(source.contains("background(indicatorColor, indicatorShape)"))
        assertFalse(source.contains("rememberCombinedBackdrop("))
        assertFalse(source.contains("shellBackdrop"))
        assertTrue(source.contains("val contentBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains(".layerBackdrop(contentBackdrop)"))
        assertTrue(source.contains("val useIndicatorBackdrop = liquidGlassEnabled && motionProgress > 0f"))
        assertTrue(source.contains("drawBackdrop("))
        assertTrue(source.contains("backdrop = contentBackdrop"))
        assertTrue(source.contains("shape = { containerShape }"))
        assertTrue(source.contains("lens("))
        assertTrue(source.contains("chromaticAberration = true"))
        assertTrue(source.contains("Highlight.Default.copy(alpha = motionProgress)"))
        assertTrue(source.contains("Shadow(alpha = if (liquidGlassEnabled) motionProgress else 0f)"))
        assertTrue(source.contains("InnerShadow("))
        assertTrue(source.contains("getBottomBarLiquidGlassEnabled("))
        assertTrue(source.contains("getLiquidGlassStyle("))
        assertTrue(source.contains("storedLiquidGlassEnabled && liquidGlassEffectsEnabled"))
        assertTrue(source.contains("if (enabled && itemCount > 1)"))
        assertTrue(source.contains("consumePointerChanges = dragSelectionEnabled"))
        assertTrue(source.contains("notifyIndexChanged = dragSelectionEnabled"))
        assertTrue(source.contains("settleIndex = if (dragSelectionEnabled) null else safeSelectedIndex"))
        assertFalse(source.contains("indicatorEffectProgress"))
        assertFalse(source.contains("backdrop = if (shouldRefractContent)"))
        assertFalse(source.contains("backdrop = shellBackdrop"))
        assertFalse(source.contains(".clip(containerShape)"))
        assertTrue(source.contains(".offset(x = segmentWidth * dragState.value)"))
        assertTrue(source.contains("78f / 56f"))
        assertTrue(source.contains("dragState.velocity / 10f"))
        assertTrue(source.contains("resolveBottomBarItemMotionVisual("))
        val indicatorIndex = source.indexOf("drawBackdrop(")
        val visibleLabelsIndex = source.indexOf(
            "selectionEmphasis = refractionMotionProfile.visibleSelectionEmphasis",
            startIndex = indicatorIndex
        )
        assertTrue(indicatorIndex >= 0)
        assertTrue(visibleLabelsIndex > indicatorIndex)
        assertFalse(source.contains("LiquidIndicator("))
        assertFalse(source.contains("resolveBottomBarIndicatorPolicy(itemCount = itemCount)"))
        assertFalse(source.contains("indicatorWidthMultiplier = 0.92f"))
        assertFalse(source.contains("maxScale = 1.06f"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
