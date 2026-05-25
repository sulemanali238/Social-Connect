package com.example.socialconnect

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable

class TilePatternDrawable(context: Context) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D9E75")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 46 // ~18% opacity
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D9E75")
        style = Paint.Style.FILL
        alpha = 31 // ~12% opacity
    }

    private val tileSize = context.resources.displayMetrics.density * 48

    override fun draw(canvas: Canvas) {
        val cols = (bounds.width() / tileSize).toInt() + 2
        val rows = (bounds.height() / tileSize).toInt() + 2

        for (col in 0..cols) {
            for (row in 0..rows) {
                val cx = col * tileSize
                val cy = row * tileSize
                val half = tileSize / 2f

                val path = Path().apply {
                    moveTo(cx + half, cy)
                    lineTo(cx + tileSize, cy + half)
                    lineTo(cx + half, cy + tileSize)
                    lineTo(cx, cy + half)
                    close()
                }
                canvas.drawPath(path, paint)
                canvas.drawCircle(cx + half, cy + half, 4f, dotPaint)
            }
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}