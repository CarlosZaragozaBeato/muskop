package com.muskop.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TabDisplayTest {
    @Test fun `measure lengths use quarter note coordinates`() {
        assertEquals(4.0, tabMeasureLength(null))
        assertEquals(3.0, tabMeasureLength("3/4"))
        assertEquals(3.5, tabMeasureLength("7/8"))
        assertEquals(1.5, tabMeasureLength("6/16"))
        assertNull(tabMeasureLength("free"))
        assertNull(tabMeasureLength("3/0"))
    }

    @Test fun `string count follows tuning and notes with sensible bounds`() {
        assertEquals(6, tabDisplayStringCount(0, emptyList()))
        assertEquals(4, tabDisplayStringCount(4, listOf(1, 4)))
        assertEquals(8, tabDisplayStringCount(7, listOf(8)))
        assertEquals(6, tabDisplayStringCount(0, listOf(25)))
    }
}
