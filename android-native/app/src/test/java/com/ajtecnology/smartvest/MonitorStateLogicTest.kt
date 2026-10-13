package com.ajtecnology.smartvest

import com.ajtecnology.smartvest.data.TelemetryReading
import com.ajtecnology.smartvest.ui.MonitorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorStateLogicTest {

    @Test
    fun testStaleDataDetection() {
        val now = 100_000L
        val readingRecent = TelemetryReading(deviceId = "VEST-001", lastUpdate = now - 10_000L)
        val readingStale = TelemetryReading(deviceId = "VEST-001", lastUpdate = now - 35_000L)

        val isRecentStale = (now - readingRecent.lastUpdate) > 30_000L
        val isStaleStale = (now - readingStale.lastUpdate) > 30_000L

        assertEquals(false, isRecentStale)
        assertEquals(true, isStaleStale)
    }

    @Test
    fun testSosStateFlag() {
        val readingSos = TelemetryReading(deviceId = "VEST-001", sosActive = true, lastUpdate = 50_000L)
        val state = MonitorUiState.Ready(readingSos, isStale = false)

        assertTrue(state.reading.sosActive)
    }
}
