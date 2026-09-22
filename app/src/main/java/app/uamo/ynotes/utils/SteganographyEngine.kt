package app.uamo.ynotes.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * SteganographyEngine provides high-performance LSB / spatial steganography
 * to conceal encrypted payloads inside image bitmaps without visual distortion.
 * All CPU-intensive pixel array operations run strictly on Dispatchers.Default.
 */
object SteganographyEngine {

    private const val HEADER_MAGIC = 0x594E5453 // "YNTS" in hex
    private const val HEADER_SIZE_BYTES = 8 // 4 bytes magic + 4 bytes length

    /**
     * Embeds a byte payload inside a carrier Bitmap.
     * Returns a new Bitmap with embedded secret data.
     */
    suspend fun embedPayload(carrier: Bitmap, payload: ByteArray): Result<Bitmap> = withContext(Dispatchers.Default) {
        runCatching {
            val totalBytes = HEADER_SIZE_BYTES + payload.size
            val bitsNeeded = totalBytes * 8
            val totalPixels = carrier.width * carrier.height
            // Each pixel has 3 color channels (R, G, B) -> 3 bits per pixel
            val availableBits = totalPixels * 3

            require(bitsNeeded <= availableBits) {
                "Carrier image too small: needs $bitsNeeded bits but only has $availableBits bits capacity"
            }

            // Prepare header + payload
            val buffer = ByteBuffer.allocate(totalBytes)
            buffer.putInt(HEADER_MAGIC)
            buffer.putInt(payload.size)
            buffer.put(payload)
            val fullBytes = buffer.array()

            // Mutable copy in ARGB_8888
            val outputBitmap = carrier.copy(Bitmap.Config.ARGB_8888, true)
            val width = outputBitmap.width
            val height = outputBitmap.height

            val pixels = IntArray(totalPixels)
            outputBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            var byteIndex = 0
            var bitIndex = 0
            var pixelIndex = 0
            var channelIndex = 0 // 0 = R, 1 = G, 2 = B

            while (byteIndex < totalBytes && pixelIndex < totalPixels) {
                val currentByte = fullBytes[byteIndex].toInt()
                val bit = (currentByte shr (7 - bitIndex)) and 1

                val pixel = pixels[pixelIndex]
                var a = (pixel shr 24) and 0xFF
                var r = (pixel shr 16) and 0xFF
                var g = (pixel shr 8) and 0xFF
                var b = pixel and 0xFF

                when (channelIndex) {
                    0 -> r = (r and 0xFE) or bit
                    1 -> g = (g and 0xFE) or bit
                    2 -> b = (b and 0xFE) or bit
                }

                pixels[pixelIndex] = (a shl 24) or (r shl 16) or (g shl 8) or b

                channelIndex++
                if (channelIndex > 2) {
                    channelIndex = 0
                    pixelIndex++
                }

                bitIndex++
                if (bitIndex > 7) {
                    bitIndex = 0
                    byteIndex++
                }
            }

            outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            outputBitmap
        }
    }

    /**
     * Extracts a hidden byte payload from a steganographic Bitmap.
     */
    suspend fun extractPayload(stegoBitmap: Bitmap): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val width = stegoBitmap.width
            val height = stegoBitmap.height
            val totalPixels = width * height

            val pixels = IntArray(totalPixels)
            stegoBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Extract header first
            var pixelIndex = 0
            var channelIndex = 0
            val headerBytes = ByteArray(HEADER_SIZE_BYTES)

            for (i in 0 until HEADER_SIZE_BYTES) {
                var currentByte = 0
                for (bit in 0 until 8) {
                    val pixel = pixels[pixelIndex]
                    val bitVal = when (channelIndex) {
                        0 -> ((pixel shr 16) and 0xFF) and 1
                        1 -> ((pixel shr 8) and 0xFF) and 1
                        else -> (pixel and 0xFF) and 1
                    }
                    currentByte = (currentByte shl 1) or bitVal

                    channelIndex++
                    if (channelIndex > 2) {
                        channelIndex = 0
                        pixelIndex++
                    }
                }
                headerBytes[i] = currentByte.toByte()
            }

            val headerBuffer = ByteBuffer.wrap(headerBytes)
            val magic = headerBuffer.int
            require(magic == HEADER_MAGIC) { "No valid yNotes steganography header found in image" }

            val payloadLength = headerBuffer.int
            require(payloadLength >= 0 && payloadLength <= (totalPixels * 3) / 8) {
                "Invalid payload length: $payloadLength"
            }

            val payload = ByteArray(payloadLength)
            for (i in 0 until payloadLength) {
                var currentByte = 0
                for (bit in 0 until 8) {
                    val pixel = pixels[pixelIndex]
                    val bitVal = when (channelIndex) {
                        0 -> ((pixel shr 16) and 0xFF) and 1
                        1 -> ((pixel shr 8) and 0xFF) and 1
                        else -> (pixel and 0xFF) and 1
                    }
                    currentByte = (currentByte shl 1) or bitVal

                    channelIndex++
                    if (channelIndex > 2) {
                        channelIndex = 0
                        pixelIndex++
                    }
                }
                payload[i] = currentByte.toByte()
            }

            payload
        }
    }
}
