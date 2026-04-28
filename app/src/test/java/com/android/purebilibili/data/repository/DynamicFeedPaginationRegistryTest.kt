package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFeedPaginationRegistryTest {

    @Test
    fun states_areIsolatedBetweenScopes() {
        val registry = DynamicFeedPaginationRegistry()

        registry.update(
            scope = DynamicFeedScope.HOME_FOLLOW,
            type = "all",
            offset = "home_offset",
            updateBaseline = "home_baseline",
            hasMore = false
        )
        registry.update(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = "all",
            offset = "dynamic_offset",
            updateBaseline = "dynamic_baseline",
            hasMore = true
        )

        assertEquals("home_offset", registry.offset(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("dynamic_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN))
        assertEquals("home_baseline", registry.updateBaseline(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("dynamic_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN))
        assertFalse(registry.hasMore(DynamicFeedScope.HOME_FOLLOW))
        assertTrue(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN))
    }

    @Test
    fun reset_onlyAffectsTargetScope() {
        val registry = DynamicFeedPaginationRegistry()
        registry.update(
            DynamicFeedScope.HOME_FOLLOW,
            type = "all",
            offset = "home_offset",
            updateBaseline = "home_baseline",
            hasMore = false
        )
        registry.update(
            DynamicFeedScope.DYNAMIC_SCREEN,
            type = "all",
            offset = "dynamic_offset",
            updateBaseline = "dynamic_baseline",
            hasMore = false
        )

        registry.reset(DynamicFeedScope.HOME_FOLLOW)

        assertEquals("", registry.offset(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("", registry.updateBaseline(DynamicFeedScope.HOME_FOLLOW))
        assertTrue(registry.hasMore(DynamicFeedScope.HOME_FOLLOW))
        assertEquals("dynamic_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN))
        assertEquals("dynamic_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN))
        assertFalse(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN))
    }

    @Test
    fun states_areIsolatedBetweenDynamicTypes() {
        val registry = DynamicFeedPaginationRegistry()

        registry.update(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = "pgc",
            offset = "pgc_offset",
            updateBaseline = "pgc_baseline",
            hasMore = false
        )
        registry.update(
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = "all",
            offset = "all_offset",
            updateBaseline = "all_baseline",
            hasMore = true
        )

        assertEquals("pgc_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN, type = "pgc"))
        assertEquals("pgc_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN, type = "pgc"))
        assertFalse(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN, type = "pgc"))
        assertEquals("all_offset", registry.offset(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
        assertEquals("all_baseline", registry.updateBaseline(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
        assertTrue(registry.hasMore(DynamicFeedScope.DYNAMIC_SCREEN, type = "all"))
    }
}
