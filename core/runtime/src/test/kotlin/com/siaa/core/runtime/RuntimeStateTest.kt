package com.siaa.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeStateTest {
    @Test fun snapshotDefaultsAreIdle() {
        val s = RuntimeSnapshot()
        assertEquals(LessonState.IDLE, s.state)
        assertEquals(0, s.completedItems)
        assertTrue(s.error == null)
    }

    @Test fun sessionConfigBoundsAreCallerControlled() {
        val c = SessionConfig(maxItems = 40, announceControls = false)
        assertEquals(40, c.maxItems)
        assertTrue(!c.announceControls)
    }
}
