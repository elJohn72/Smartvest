package com.ajtecnology.smartvest

import com.ajtecnology.smartvest.data.copyCameraLuminance
import com.ajtecnology.smartvest.data.parseVestSetupQr
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class VestSetupInputTest {
    @Test fun paddedCameraRowsDoNotCorruptQrPixels() {
        val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 99, 99, 3, 4, 99, 99))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), copyCameraLuminance(buffer, 2, 2, 4, 1))
        assertEquals(0, buffer.position())
    }
    @Test fun cameraPixelStrideAndBufferOffsetAreRespected() {
        val buffer = ByteBuffer.wrap(byteArrayOf(99, 99, 1, 88, 2, 88, 3, 88, 4))
        buffer.position(2)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), copyCameraLuminance(buffer, 2, 2, 4, 2))
        assertEquals(2, buffer.position())
    }
    @Test fun invalidCameraPlanesAreRejectedWithoutReadingOutsideBuffer() {
        assertNull(copyCameraLuminance(ByteBuffer.wrap(byteArrayOf(1)), 2, 2, 2, 1))
        assertNull(copyCameraLuminance(ByteBuffer.allocate(8), 2, 2, 1, 1))
        assertNull(copyCameraLuminance(ByteBuffer.allocate(8), Int.MAX_VALUE, 2, 2, 1))
    }
    @Test fun proofOfPossessionIsNeverSilentlyRewritten() {
        val template = """{"ver":"v1","name":"PROV_SmartVest_70C4","pop":"%s","transport":"ble"}"""
        assertNotNull(parseVestSetupQr(template.format("A7K9M2PX")))
        assertNull(parseVestSetupQr(template.format("a7k9-m2px")))
        assertNull(parseVestSetupQr(template.format("A7K9M2P!")))
        assertNull(parseVestSetupQr(" ".repeat(1100) + template.format("A7K9M2PX")))
    }
}
