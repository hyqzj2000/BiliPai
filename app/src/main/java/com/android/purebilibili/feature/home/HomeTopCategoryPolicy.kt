package com.android.purebilibili.feature.home

private val HOME_TOP_CATEGORIES = listOf(
    HomeCategory.RECOMMEND,
    HomeCategory.FOLLOW,
    HomeCategory.POPULAR,
    HomeCategory.LIVE,
    HomeCategory.GAME
)

fun resolveHomeTopCategories(): List<HomeCategory> = HOME_TOP_CATEGORIES

fun resolveHomeTopTabIndex(category: HomeCategory): Int {
    return HOME_TOP_CATEGORIES.indexOf(category).takeIf { it >= 0 } ?: 0
}

fun resolveHomeCategoryForTopTab(index: Int): HomeCategory {
    return HOME_TOP_CATEGORIES.getOrNull(index) ?: HomeCategory.RECOMMEND
}
