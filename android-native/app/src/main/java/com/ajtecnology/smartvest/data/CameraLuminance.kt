package com.ajtecnology.smartvest.data

import java.nio.ByteBuffer

/** Pack the Y plane without assuming that CameraX rows/pixels are contiguous. */
fun copyCameraLuminance(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int): ByteArray? {
    if (width <= 0 || height <= 0 || pixelStride <= 0 || rowStride <= 0) return null
    if (width.toLong() * height > 20_000_000L) return null
    val rowBytes = (width - 1L) * pixelStride + 1
    if (rowBytes > rowStride) return null
    val needed = (height - 1L) * rowStride + rowBytes
    if (needed > buffer.remaining()) return null
    val source = buffer.duplicate()
    val start = source.position()
    val result = ByteArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) result[y * width + x] = source.get(start + y * rowStride + x * pixelStride)
    }
    return result
}
