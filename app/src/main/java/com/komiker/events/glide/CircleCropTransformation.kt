package com.komiker.events.glide

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader.TileMode
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest
import kotlin.math.min

class CircleCropTransformation : BitmapTransformation() {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray())
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        // Calculate the size for the square bitmap and the offset
        val size = min(toTransform.width, toTransform.height)
        val x = (toTransform.width - size) / 2
        val y = (toTransform.height - size) / 2

        // Create a square bitmap from the original bitmap
        val squared = Bitmap.createBitmap(toTransform, x, y, size, size)

        // Use the BitmapPool to get a new bitmap with the same size and configuration
        val result = pool.get(size, size, Bitmap.Config.ARGB_8888)

        // Draw the circle cropped bitmap onto the result bitmap
        val canvas = Canvas(result)
        val paint = Paint()
        val shader = BitmapShader(squared, TileMode.CLAMP, TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        // No need to recycle 'squared' here; it's handled by Glide
        // 'result' is returned to Glide, and Glide handles its recycling

        return result
    }

    companion object {
        private const val ID = "com.komiker.events.glide.CircleCropTransformation"
    }
}