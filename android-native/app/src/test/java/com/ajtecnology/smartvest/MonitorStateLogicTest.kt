package com.ajtecnology.smartvest

import com.ajtecnology.smartvest.data.TelemetryReading
import com.ajtecnology.smartvest.ui.DistanceBand
import com.ajtecnology.smartvest.ui.MonitorUiState
import com.ajtecnology.smartvest.ui.batteryLabel
import com.ajtecnology.smartvest.ui.distanceBand
import com.ajtecnology.smartvest.ui.distanceBandLabel
import com.ajtecnology.smartvest.ui.gpsLabel
import com.ajtecnology.smartvest.ui.hasValidGps
import com.ajtecnology.smartvest.ui.historyAgeLabel
import com.ajtecnology.smartvest.ui.isReadingStale
import com.ajtecnology.smartvest.ui.readingAgeLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorStateLogicTest {

    @Test
    fun testStaleDataDetection() {
        val now = 100_000L
        val readingRecent = TelemetryReading(deviceId = "VEST-001", lastUpdate = now - 10_000L)
        val readingStale = TelemetryReading(deviceId = "VEST-001", lastUpdate = now - 35_000L)

        assertFalse(isReadingStale(now, readingRecent.lastUpdate))
        assertTrue(isReadingStale(now, readingStale.lastUpdate))
        assertFalse(isReadingStale(now, now - 30_000L))
        assertTrue(isReadingStale(now, 0L))
    }

    @Test
    fun testDistanceBandsUseWords() {
        assertEquals(DistanceBand.DANGER, distanceBand(40f))
        assertEquals("Peligro", distanceBandLabel(40f))
        assertEquals(DistanceBand.ALERT, distanceBand(41f))
        assertEquals("Alerta", distanceBandLabel(100f))
        assertEquals(DistanceBand.CAUTION, distanceBand(101f))
        assertEquals("Precaución", distanceBandLabel(200f))
        assertEquals(DistanceBand.CLEAR, distanceBand(201f))
        assertEquals("Despejado", distanceBandLabel(201f))
        assertEquals(DistanceBand.UNKNOWN, distanceBand(null))
        assertEquals("Sin lectura", distanceBandLabel(null))
    }

    @Test
    fun testUnknownBatteryAndGpsStayEmpty() {
        assertEquals("Sin dato", batteryLabel(null))
        assertEquals("80 %", batteryLabel(80))
        assertEquals("Sin fijación", gpsLabel(false, 0.1, -79.0))
        assertEquals("Sin fijación", gpsLabel(true, null, null))
        assertFalse(hasValidGps(true, 95.0, 10.0))
        assertTrue(gpsLabel(true, 0.123456, -79.123456).startsWith("Fijación 0.12346, -79.12346"))
    }

    @Test
    fun testReadingAgeAndHistoryAge() {
        val now = 200_000L
        assertEquals("Sin hora de lectura", readingAgeLabel(now, 0L))
        assertEquals("hace 12 s", readingAgeLabel(now, now - 12_000L))
        assertEquals("hace 2 min", readingAgeLabel(now, now - 120_000L))
        val recorded = "2023-11-14T22:13:20Z"
        val then = java.time.Instant.parse(recorded).toEpochMilli()
        assertEquals("hace 3 s", historyAgeLabel(then + 3_000L, recorded))
        assertEquals("no-es-fecha", historyAgeLabel(now, "no-es-fecha"))
    }

    @Test
    fun testSosStateFlag() {
        val readingSos = TelemetryReading(deviceId = "VEST-001", sosActive = true, lastUpdate = 50_000L)
        val state = MonitorUiState.Ready(readingSos, isStale = false)

        assertTrue(state.reading.sosActive)
    }
}
