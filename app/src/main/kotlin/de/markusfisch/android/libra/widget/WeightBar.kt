package de.markusfisch.android.libra.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet
import de.markusfisch.android.libra.R
import kotlin.math.abs
import kotlin.math.roundToInt

class WeightBar : AppCompatSeekBar {
	private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val positiveColor: Int
	private val negativeColor: Int
	private val neutralColor: Int

	private var step = 0
	private var base = 0

	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle) {
		val res = context.resources
		val dp = res.displayMetrics.density
		textPaint.textSize = 12f * dp
		positiveColor = ContextCompat.getColor(context, R.color.yes)
		negativeColor = ContextCompat.getColor(context, R.color.no)
		neutralColor = ContextCompat.getColor(context, R.color.neutral)
	}

	constructor(context: Context, attrs: AttributeSet) :
			this(context, attrs, 0)

	override fun onLayout(
		changed: Boolean,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int
	) {
		super.onLayout(changed, left, top, right, bottom)
		val width = right - left
		val height = bottom - top
		step = ((width - paddingLeft - paddingRight) / max.toFloat()).roundToInt()
		base = height / 2
	}

	override fun onDraw(canvas: Canvas) {
		drawLabels(canvas)
		super.onDraw(canvas)
	}

	private fun drawLabels(canvas: Canvas) {
		var x: Int = paddingLeft
		val half = max / 2
		for (i in -half..half) {
			val s = abs(i).toString()
			val bounds = Rect()
			textPaint.getTextBounds(s, 0, s.length, bounds)
			textPaint.color = when {
				i < 0 -> negativeColor
				i > 0 -> positiveColor
				else -> neutralColor
			}
			canvas.drawText(
				s,
				(x - bounds.centerX()).toFloat(),
				(base - bounds.centerY()).toFloat(),
				textPaint
			)
			x += step
		}
	}
}
