package com.example.socialconnect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * HeatmapGridView
 *
 * Renders a 7-column × N-row grid of coloured cells representing daily activity.
 * Call [setData] with a flat list of intensity values (0–4) ordered Mon→Sun,
 * week by week.  The view sizes itself to exactly fit all rows with a fixed
 * cell height + gap.
 */
class HeatmapGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Appearance ──────────────────────────────────────────────────────────
    private val cellHeightDp = 22f
    private val gapDp        = 4f
    private val cornerDp     = 4f

    private val intensityColors = intArrayOf(
        Color.parseColor("#E1F5EE"), // 0 – lightest
        Color.parseColor("#9FE1CB"), // 1
        Color.parseColor("#5DCAA5"), // 2
        Color.parseColor("#1D9E75"), // 3
        Color.parseColor("#0F6E56")  // 4 – darkest
    )

    // ── State ────────────────────────────────────────────────────────────────
    private var values: List<Int> = emptyList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect  = RectF()

    // ── Public API ────────────────────────────────────────────────────────────
    /**
     * @param data flat list of intensity values 0–4, Mon-first, 7 per week.
     *             Length should be a multiple of 7.
     */
    fun setData(data: List<Int>) {
        values = data
        requestLayout()
        invalidate()
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val cellH   = cellHeightDp * density
        val gap     = gapDp * density

        val cols  = 7
        val rows  = if (values.isEmpty()) 0 else Math.ceil(values.size / cols.toDouble()).toInt()
        val totalH = (rows * (cellH + gap) - gap).coerceAtLeast(0f)

        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, totalH.toInt())
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val density = resources.displayMetrics.density
        val cellH   = cellHeightDp * density
        val gap     = gapDp * density
        val corner  = cornerDp * density
        val cols    = 7
        val cellW   = (width - gap * (cols - 1)) / cols

        values.forEachIndexed { index, intensity ->
            val col = index % cols
            val row = index / cols

            val left   = col * (cellW + gap)
            val top    = row * (cellH + gap)
            val right  = left + cellW
            val bottom = top + cellH

            paint.color = intensityColors[intensity.coerceIn(0, 4)]
            rect.set(left, top, right, bottom)
            canvas.drawRoundRect(rect, corner, corner, paint)
        }
    }
}