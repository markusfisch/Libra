package de.markusfisch.android.libra.widget

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatTextView
import android.text.TextPaint
import android.util.AttributeSet
import android.widget.RelativeLayout
import de.markusfisch.android.libra.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class ArgumentView : AppCompatTextView {
	var id = 0L
	var weight: Int = 0
		set(value) {
			layoutParams = when {
				value < 0 -> {
					setPadding(
						outerPaddingHorizontalLarge,
						outerPaddingVertical,
						outerPaddingHorizontalSmall,
						outerPaddingVertical
					)
					getParams(RelativeLayout.ALIGN_PARENT_LEFT)
				}
				value > 0 -> {
					setPadding(
						outerPaddingHorizontalSmall,
						outerPaddingVertical,
						outerPaddingHorizontalLarge,
						outerPaddingVertical
					)
					getParams(RelativeLayout.ALIGN_PARENT_RIGHT)
				}
				else -> {
					setPadding(
						outerPaddingHorizontalSmall,
						outerPaddingVertical,
						outerPaddingHorizontalSmall,
						outerPaddingVertical
					)
					getParams(RelativeLayout.CENTER_IN_PARENT)
				}
			}
			field = value
		}

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
	private val textBounds = Rect()
	private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val bubbleRadius: Float
	private val bubbleRect = RectF()
	private val triangleSize: Float
	private val negativeTriangle = Path()
	private val positiveTriangle = Path()
	private val outerPaddingVertical: Int
	private val outerPaddingHorizontalSmall: Int
	private val outerPaddingHorizontalLarge: Int
	private val maxRadius: Int
	private val innerPaddingVertical: Float
	private val innerPaddingHorizontal: Float
	private val positiveColor: Int
	private val negativeColor: Int

	private var positiveX = 0
	private var negativeX = 0
	private var base = 0

	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle) {
		val res = context.resources
		val dp = res.displayMetrics.density
		textPaint.textSize = 12f * dp
		textPaint.typeface = Typeface.DEFAULT_BOLD
		textPaint.color = ContextCompat.getColor(context, R.color.background_window)
		bubblePaint.color = ContextCompat.getColor(context, R.color.bubble)
		bubbleRadius = 4f * dp
		triangleSize = 6f * dp
		outerPaddingVertical = (8f * dp).roundToInt()
		outerPaddingHorizontalSmall = (24f * dp).roundToInt()
		outerPaddingHorizontalLarge = (62f * dp).roundToInt()
		maxRadius = (8f * dp).roundToInt()
		innerPaddingVertical = 4f * dp
		innerPaddingHorizontal = 8f * dp
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
		positiveX = width - outerPaddingHorizontalSmall - maxRadius
		negativeX = outerPaddingHorizontalSmall + maxRadius
		bubbleRect.set(
			paddingLeft - innerPaddingHorizontal,
			innerPaddingVertical,
			width - paddingRight + innerPaddingHorizontal,
			height - innerPaddingVertical
		)
		val triTop = bubbleRect.centerY() - triangleSize
		val triBottom = bubbleRect.centerY() + triangleSize
		negativeTriangle.apply {
			reset()
			moveTo(bubbleRect.left, triTop)
			lineTo(bubbleRect.left - triangleSize, bubbleRect.centerY())
			lineTo(bubbleRect.left, triBottom)
			close()
		}
		positiveTriangle.apply {
			reset()
			moveTo(bubbleRect.right, triTop)
			lineTo(bubbleRect.right + triangleSize, bubbleRect.centerY())
			lineTo(bubbleRect.right, triBottom)
			close()
		}
		base = height / 2
	}

	override fun onDraw(canvas: Canvas) {
		drawWeight(canvas)
		super.onDraw(canvas)
	}

	private fun drawWeight(canvas: Canvas) {
		canvas.drawRoundRect(
			bubbleRect,
			bubbleRadius,
			bubbleRadius,
			bubblePaint
		)
		if (weight == 0) {
			return
		}
		val triangle: Path
		val x: Int
		paint.color = if (weight > 0) {
			triangle = positiveTriangle
			x = positiveX
			positiveColor
		} else {
			triangle = negativeTriangle
			x = negativeX
			negativeColor
		}
		canvas.drawPath(triangle, bubblePaint)
		canvas.drawCircle(
			x.toFloat(),
			base.toFloat(),
			(maxRadius + maxRadius / 16f * abs(weight).toFloat()),
			paint
		)
		val s = abs(min(weight, 10)).toString()
		textPaint.getTextBounds(s, 0, s.length, textBounds)
		canvas.drawText(
			s,
			(x - textBounds.centerX()).toFloat(),
			(base - textBounds.centerY()).toFloat(),
			textPaint
		)
	}
}

private fun getParams(flag: Int): RelativeLayout.LayoutParams {
	val params = RelativeLayout.LayoutParams(
		RelativeLayout.LayoutParams.WRAP_CONTENT,
		RelativeLayout.LayoutParams.WRAP_CONTENT
	)
	params.addRule(flag)
	return params
}
