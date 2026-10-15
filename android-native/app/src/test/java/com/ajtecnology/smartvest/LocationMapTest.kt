package com.ajtecnology.smartvest

import com.ajtecnology.smartvest.ui.*
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class LocationMapTest {
    @Test fun invalidCoordinatesNeverReachJavascript() {
        for (point in listOf(MapLocation(Double.NaN, 0.0), MapLocation(0.0, Double.POSITIVE_INFINITY), MapLocation(91.0, 0.0), MapLocation(0.0, -181.0))) {
            assertNull(locationUpdateScript(point, "#1E40AF"))
        }
    }
    @Test fun updatesUseNumericLiteralsRegardlessOfPhoneLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("window.smartvestSetLocation(0.123456,-79.123456,'#1E40AF');", locationUpdateScript(MapLocation(0.123456, -79.123456), "#1E40AF"))
            assertEquals("window.smartvestSetLocation(0.123456,-79.123456,'#1E40AF');", locationUpdateScript(MapLocation(0.123456, -79.123456), "');alert(1);//"))
        } finally { Locale.setDefault(previous) }
    }
    @Test fun mapEngineIsBundledAndOnlyTilesCanLoadRemotely() {
        val document = locationMapDocument("LOCAL_CSS", "LOCAL_ENGINE", "LOCAL_CONTROLLER")
        assertTrue(document.contains("LOCAL_CSS"))
        assertTrue(document.contains("LOCAL_ENGINE"))
        assertTrue(document.contains("LOCAL_CONTROLLER"))
        assertFalse(document.contains("unpkg.com"))
        assertFalse(document.contains("<script src="))
        assertTrue(document.contains("img-src https://tile.openstreetmap.org data:"))
        assertTrue(document.contains("Reintentar"))
    }
}
