package io.github.ephym6.plantid.data.identification.model

import android.content.Context
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.size.Scale
import androidx.core.graphics.scale
import coil3.toBitmap
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Turns a photo into the model's input, following `model_metadata.json`:
 * 1. apply EXIF orientation (Coil does this when decoding);
 * 2. centre-crop the largest square;
 * 3. resize to [inputSize] x [inputSize] (bilinear);
 * 4. raw RGB 0–255 as float32, **no normalisation** (the model does it).
 *
 * The photo is decoded at a reduced size first (shorter side at least
 * 2 x [inputSize]) to avoid loading full-resolution images into memory.
 */
class ImagePreprocessor(context: Context, private val inputSize: Int) {

    private val appContext = context.applicationContext

    /** Model input for [photo], or null if it can't be decoded. */
    suspend fun toInput(photo: File): ByteBuffer? {
        val decodeSize = inputSize * DECODE_FACTOR
        val request = ImageRequest.Builder(appContext)
            .data(photo)
            .size(decodeSize, decodeSize)
            .scale(Scale.FILL) // shorter side >= decodeSize, aspect ratio kept
            .precision(Precision.INEXACT)
            .allowHardware(false) // pixels must be readable
            .memoryCachePolicy(CachePolicy.DISABLED) // used once; don't keep it in memory
            .build()
        val decoded = (SingletonImageLoader.get(appContext).execute(request) as? SuccessResult)
            ?.image?.toBitmap() ?: return null

        val side = min(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        val scaled = square.scale(inputSize, inputSize, filter = true) // bilinear
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        return toFloatRgbBuffer(pixels)
    }

    companion object {
        private const val DECODE_FACTOR = 2

        /** Packs ARGB pixels (row-major) into a direct float32 RGB buffer, values 0–255. */
        fun toFloatRgbBuffer(pixels: IntArray): ByteBuffer {
            val buffer = ByteBuffer.allocateDirect(pixels.size * 3 * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
            for (argb in pixels) {
                buffer.putFloat(((argb shr 16) and 0xFF).toFloat())
                buffer.putFloat(((argb shr 8) and 0xFF).toFloat())
                buffer.putFloat((argb and 0xFF).toFloat())
            }
            buffer.rewind()
            return buffer
        }
    }
}
