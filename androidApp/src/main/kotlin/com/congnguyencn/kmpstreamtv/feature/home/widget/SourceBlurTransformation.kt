package com.congnguyencn.kmpstreamtv.feature.home.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.max

/**
 * Coil equivalent of the reference app's `BlurTransformation(5, 25)`.
 * The bitmap is downsampled before blur exactly as in that implementation.
 */
internal class SourceBlurTransformation(
    context: Context,
    private val radius: Int,
    private val sampling: Int,
) : Transformation() {
    private val applicationContext = context.applicationContext

    override val cacheKey = "source-blur-radius=$radius-sampling=$sampling"

    @Suppress("DEPRECATION")
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val output = Bitmap.createBitmap(
            max(1, input.width / sampling),
            max(1, input.height / sampling),
            Bitmap.Config.ARGB_8888,
        )
        Canvas(output).run {
            scale(1f / sampling, 1f / sampling)
            drawBitmap(input, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        val renderScript = RenderScript.create(applicationContext)
        val inputAllocation = Allocation.createFromBitmap(renderScript, output)
        val outputAllocation = Allocation.createTyped(renderScript, inputAllocation.type)
        val blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        try {
            blur.setRadius(radius.toFloat().coerceIn(0.1f, 25f))
            blur.setInput(inputAllocation)
            blur.forEach(outputAllocation)
            outputAllocation.copyTo(output)
        } finally {
            blur.destroy()
            outputAllocation.destroy()
            inputAllocation.destroy()
            renderScript.destroy()
        }
        return output
    }
}
