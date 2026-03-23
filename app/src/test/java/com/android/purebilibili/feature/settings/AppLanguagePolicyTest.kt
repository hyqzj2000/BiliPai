package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppLanguagePolicyTest {

    @Test
    fun invalidRawValue_fallsBackToFollowSystem() {
        assertEquals(
            AppLanguage.FOLLOW_SYSTEM,
            resolveAppLanguagePreference(rawValue = 999)
        )
    }

    @Test
    fun explicitLanguageValues_mapToExpectedOptions() {
        assertEquals(
            AppLanguage.SIMPLIFIED_CHINESE,
            resolveAppLanguagePreference(rawValue = AppLanguage.SIMPLIFIED_CHINESE.value)
        )
        assertEquals(
            AppLanguage.TRADITIONAL_CHINESE_TAIWAN,
            resolveAppLanguagePreference(rawValue = AppLanguage.TRADITIONAL_CHINESE_TAIWAN.value)
        )
        assertEquals(
            AppLanguage.ENGLISH,
            resolveAppLanguagePreference(rawValue = AppLanguage.ENGLISH.value)
        )
    }

    @Test
    fun followSystem_usesEmptyLocaleTags() {
        assertTrue(resolveAppLanguageLocaleTags(AppLanguage.FOLLOW_SYSTEM).isEmpty())
    }

    @Test
    fun explicitLanguages_mapToExpectedLocaleTags() {
        assertEquals(
            listOf("zh-CN"),
            resolveAppLanguageLocaleTags(AppLanguage.SIMPLIFIED_CHINESE)
        )
        assertEquals(
            listOf("zh-TW"),
            resolveAppLanguageLocaleTags(AppLanguage.TRADITIONAL_CHINESE_TAIWAN)
        )
        assertEquals(
            listOf("en"),
            resolveAppLanguageLocaleTags(AppLanguage.ENGLISH)
        )
    }
}
