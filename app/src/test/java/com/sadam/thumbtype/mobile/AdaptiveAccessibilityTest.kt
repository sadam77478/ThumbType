package com.sadam.thumbtype.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveAccessibilityTest {
    @Test
    fun compactWidthsStayCompact() {
        assertEquals(ThumbTypeWidthClass.COMPACT, classifyThumbTypeWidth(320))
        assertEquals(ThumbTypeWidthClass.COMPACT, classifyThumbTypeWidth(599))
    }

    @Test
    fun mediumWidthsUseTabletPhoneTransitionClass() {
        assertEquals(ThumbTypeWidthClass.MEDIUM, classifyThumbTypeWidth(600))
        assertEquals(ThumbTypeWidthClass.MEDIUM, classifyThumbTypeWidth(839))
    }

    @Test
    fun expandedWidthsUseLargeScreenClass() {
        assertEquals(ThumbTypeWidthClass.EXPANDED, classifyThumbTypeWidth(840))
        assertEquals(ThumbTypeWidthClass.EXPANDED, classifyThumbTypeWidth(1400))
    }
}
