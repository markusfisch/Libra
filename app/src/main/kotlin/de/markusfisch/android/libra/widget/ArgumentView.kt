package de.markusfisch.android.libra.widget

import android.annotation.SuppressLint
import de.markusfisch.android.libra.R

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.view.Gravity
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class ArgumentView : AppCompatTextView {
	var id = 0L
	var weight: Int = 0
		// this gravity depends on design, not reading direction
		@SuppressLint("RtlHardcoded")
		set(value) {
			gravity = when {
				value < 0 -> Gravity.LEFT
				value > 0 -> Gravity.RIGHT
				else -> Gravity.CENTER_HORIZONTAL
			}
			field = value
		}

	private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val padding: Int
	private var radius: Int
	private val positiveColor: Int
	private val negativeColor: Int

	private var positiveX = 0
	private var negativeX = 0
	private var base = 0

	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle) {
		val res = context.resources
		val dp = res.displayMetrics.density
		textPaint.textSize = dp * 12f
		textPaint.typeface = Typeface.DEFAULT_BOLD
		textPaint.color = ContextCompat.getColor(context, R.color.background_window)
		padding = (dp * 24f).roundToInt()
		radius = (dp * 8f).roundToInt()
		positiveColor = ContextCompat.getColor(context, R.color.yes)
		negativeColor = ContextCompat.getColor(context, R.color.no)
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
		positiveX = width - padding - radius
		negativeX = padding + radius
		base = height / 2
	}

	override fun onDraw(canvas: Canvas) {
		drawWeights(canvas)
		super.onDraw(canvas)
	}

	private fun drawWeights(canvas: Canvas) {
		val x: Int
		paint.color = if (weight > 0) {
			x = positiveX
			positiveColor
		} else {
			x = negativeX
			negativeColor
		}
		canvas.drawCircle(
			x.toFloat(),
			base.toFloat(),
			(radius + radius / 16f * abs(weight).toFloat()),
			paint
		)
		val s = abs(min(weight, 10)).toString()
		val bounds = Rect()
		textPaint.getTextBounds(s, 0, s.length, bounds)
		canvas.drawText(
			s,
			(x - bounds.centerX()).toFloat(),
			(base - bounds.centerY()).toFloat(),
			textPaint
		)
	}
}
