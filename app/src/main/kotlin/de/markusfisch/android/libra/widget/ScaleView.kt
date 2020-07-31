package de.markusfisch.android.libra.widget

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.app.prefs
import kotlin.math.*

class ScaleView(context: Context) : View(context) {
	var radians = 0.0
		set(value) {
			invalidate()
			field = value
		}

	private val radPerDeg = Math.PI / 180.0
	private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
	private val textBounds = Rect()
	private val pnt = Paint(Paint.ANTI_ALIAS_FLAG)
	private val mat = Matrix()
	private val topMargin: Int
	private val bottomMargin: Int
	private val transparentColor: Int
	private val backgroundColor: Int
	private val yesColor: Int
	private val yesString: String
	private val maybeColor: Int
	private val maybeString: String
	private val noColor: Int
	private val noString: String
	private val negativeSumLabel: String
	private val positiveSumLabel: String
	private val frame: Bitmap
	private val frameHeight: Int
	private val frameMidX: Float
	private val frameAxis: Float
	private val scale: Bitmap
	private val scaleMidX: Float
	private val scaleMidY: Float
	private val scaleRadius: Float
	private val pan: Bitmap
	private val panMidX: Float

	private var noWeights = false
	private var negativeSum = 0
	private var positiveSum = 0

	init {
		val res = context.resources
		val dp = res.displayMetrics.density

		textPaint.typeface = Typeface.DEFAULT_BOLD
		textPaint.textSize = 18f * dp
		pnt.isFilterBitmap = true
		pnt.textSize = 12f * dp
		topMargin = (32f * dp).roundToInt()
		bottomMargin = (8f * dp).roundToInt()

		transparentColor = 0x40000000
		backgroundColor = ContextCompat.getColor(
			context,
			R.color.background_window
		)
		yesColor = ContextCompat.getColor(context, R.color.yes)
		yesString = context.getString(R.string.yes)
		maybeColor = ContextCompat.getColor(context, R.color.maybe)
		maybeString = context.getString(R.string.maybe)
		noColor = ContextCompat.getColor(context, R.color.no)
		noString = context.getString(R.string.no)
		textPaint.color = ContextCompat.getColor(context, R.color.background_window)
		negativeSumLabel = context.getString(R.string.negative_sum)
		positiveSumLabel = context.getString(R.string.positive_sum)

		frame = BitmapFactory.decodeResource(res, R.drawable.scale_frame)
		val frameWidth = frame.width
		frameHeight = frame.height
		frameMidX = (frameWidth * .5f).roundToInt().toFloat()
		frameAxis = (frameHeight * .39f).roundToInt().toFloat()

		scale = BitmapFactory.decodeResource(res, R.drawable.scale_bar)
		val scaleWidth = scale.width
		val scaleHeight = scale.height
		scaleMidX = (scaleWidth * .5f).roundToInt().toFloat()
		scaleMidY = (scaleHeight * .5f).roundToInt().toFloat()
		scaleRadius = (scaleWidth * .48f).roundToInt().toFloat()

		pan = BitmapFactory.decodeResource(res, R.drawable.scale_pan)
		val panWidth = pan.width
		panMidX = (panWidth * .5f).roundToInt().toFloat()
	}

	fun setWeights(negative: Int, positive: Int) {
		negativeSum = negative
		positiveSum = positive
		noWeights = if (negative < 0 || positive < 0) {
			radians = 0.0
			true
		} else {
			val target = calculateAngle(
				negative.toFloat(),
				positive.toFloat()
			).toDouble()
			if (target != radians) {
				if (visibility == VISIBLE) {
					animation = ScaleAnimation(this, radians, target)
				} else {
					radians = target
				}
			}
			false
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
		val desiredHeight = topMargin + frameHeight + bottomMargin
		setMeasuredDimension(
			measureDimension(desiredWidth, widthMeasureSpec),
			measureDimension(desiredHeight, heightMeasureSpec)
		)
	}

	private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
		val specMode = MeasureSpec.getMode(measureSpec)
		val specSize = MeasureSpec.getSize(measureSpec)
		return if (specMode == MeasureSpec.EXACTLY) {
			specSize
		} else {
			if (specMode == MeasureSpec.AT_MOST) {
				min(desiredSize, specSize)
			} else {
				desiredSize
			}
		}
	}

	override fun onDraw(canvas: Canvas) {
		canvas.drawColor(backgroundColor)

		val centerX = round(width / 2f)
		val centerY = round(height / 2f)
		val top = topMargin.toFloat()

		val alphaMod = if (noWeights) {
			pnt.color = transparentColor
			transparentColor
		} else {
			// toInt() is required here or Kotlin thinks it's a Long
			pnt.color = 0xffffffff.toInt()
			0xff000000.toInt()
		}

		val textPad = top * .5f
		val negativeColor = noColor and 0xffffff or alphaMod
		val positiveColor = yesColor and 0xffffff or alphaMod
		pnt.color = negativeColor
		canvas.drawText(
			noString,
			centerX - frameMidX - pnt.measureText(noString),
			top + textPad,
			pnt
		)
		pnt.color = maybeColor and 0xffffff or alphaMod
		canvas.drawText(maybeString, centerX, top - textPad * .5f, pnt)
		pnt.color = positiveColor
		canvas.drawText(yesString, centerX + frameMidX, top + textPad, pnt)

		if (prefs.showSums && !noWeights) {
			val sumPadding = scaleRadius * 2.5f
			val negativeX = round(centerX - sumPadding)
			val positiveX = round(centerX + sumPadding)
			val sumY = round(centerY)

			drawSum(
				canvas,
				"$negativeSum",
				negativeX,
				sumY,
				negativeColor,
				-radians
			)
			drawSum(
				canvas,
				"$positiveSum",
				positiveX,
				sumY,
				positiveColor,
				radians
			)
		}

		mat.setTranslate(centerX - frameMidX, top)
		canvas.drawBitmap(frame, mat, pnt)

		val topAxis = top + frameAxis
		val rx = centerX + scaleRadius * cos(radians).toFloat()
		val ry = topAxis + scaleRadius * sin(radians).toFloat()
		val lx = centerX + scaleRadius * cos(radians + Math.PI).toFloat()
		val ly = topAxis + scaleRadius * sin(radians + Math.PI).toFloat()

		mat.setTranslate(lx - panMidX, ly)
		canvas.drawBitmap(pan, mat, pnt)

		mat.setTranslate(rx - panMidX, ry)
		canvas.drawBitmap(pan, mat, pnt)

		mat.setTranslate(centerX - scaleMidX, topAxis - scaleMidY)
		mat.postRotate((radians / radPerDeg).toFloat(), centerX, topAxis)
		canvas.drawBitmap(scale, mat, pnt)
	}

	private fun drawSum(
		canvas: Canvas,
		text: String,
		x: Float,
		y: Float,
		color: Int,
		size: Double
	) {
		textPaint.getTextBounds(text, 0, text.length, textBounds)
		val range = scaleRadius * .05f
		val base = scaleRadius * .15f + range
		val radius = textBounds.diagonal().toFloat() * .5f +
				base +
				range * size.toFloat()
		pnt.color = color
		canvas.drawCircle(x, y, radius, pnt)
		val cx = textBounds.centerX().toFloat()
		val cy = textBounds.centerY().toFloat()
		canvas.drawText(text, x - cx, y - cy, textPaint)
	}
}

private fun Rect.diagonal(): Double {
	val dx = width().toDouble()
	val dy = height().toDouble()
	return sqrt(dx * dx + dy * dy)
}

private class ScaleAnimation(
	val scaleView: ScaleView,
	val fromRadians: Double,
	val toRadians: Double
) : Animation() {
	init {
		duration = 300L
	}

	override fun applyTransformation(
		interpolatedTime: Float,
		t: Transformation?
	) {
		scaleView.radians = fromRadians +
				(toRadians - fromRadians) * interpolatedTime
	}
}

private const val MAX_ANGLE = 1f
private const val DOUBLE_WEIGHT_ANGLE = .7853f
private fun calculateAngle(negative: Float, positive: Float): Float {
	val balance = positive - negative
	if (balance == 0f) {
		return 0f
	}
	val max = max(positive, negative)
	val min = min(positive, negative)
	val angle = max * DOUBLE_WEIGHT_ANGLE / (min * 2f)
	return if (positive > negative) {
		min(MAX_ANGLE, angle)
	} else {
		max(-MAX_ANGLE, -angle)
	}
}

