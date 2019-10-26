package de.markusfisch.android.libra.widget

import android.annotation.SuppressLint
import de.markusfisch.android.libra.R

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
	private val padding: Float
	private val spacing: Float
	private val positiveColor: Int
	private val negativeColor: Int

	private var width = 0f
	private var height = 0f
	private var base = 0f
	private var center = 0f
	private var radius = 0f

	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle) {
		val res = context.resources
		val dp = res.displayMetrics.density
		textPaint.textSize = dp * 10f
		textPaint.color = ContextCompat.getColor(context, R.color.background_window)
		padding = dp * 16f
		spacing = dp
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
		width = (right - left).toFloat()
		height = (bottom - top).toFloat()
		center = (width / 2f).roundToInt().toFloat()
		radius = (((center - padding * 2f) / 10f - spacing) / 2f).roundToInt().toFloat()
		base = (height - padding - radius).roundToInt().toFloat()
	}

	override fun onDraw(canvas: Canvas) {
		drawWeights(canvas)
		super.onDraw(canvas)
	}

	private fun drawWeights(canvas: Canvas) {
		var x: Float
		var step = spacing + radius * 2f
		paint.color = if (weight > 0) {
			x = width - padding - radius
			step = -step
			positiveColor
		} else {
			x = padding + radius
			negativeColor
		}
		val bounds = Rect()
		for (it in 1..abs(min(weight, 10))) {
			canvas.drawCircle(x, base, radius, paint)
			val s = it.toString()
			textPaint.getTextBounds(s, 0, s.length, bounds)
			canvas.drawText(
				s,
				x - bounds.centerX(),
				base - bounds.centerY(),
				textPaint
			)
			x += step
		}
	}
}
