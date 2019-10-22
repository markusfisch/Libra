package de.markusfisch.android.libra.widget

import de.markusfisch.android.libra.R

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.support.v4.content.ContextCompat
import android.view.View
import kotlin.math.*

class ScaleView(context: Context) : View(context) {
	private val animationTime = 300L
	private val animationRunnable: Runnable = Runnable {
		while (!Thread.currentThread().isInterrupted) {
			val now = min(
				System.currentTimeMillis() - animationStart,
				animationTime
			)

			radians = linear(
				now,
				radiansBegin,
				radiansChange,
				animationTime
			)

			invalidate()

			if (
				radiansChange == .0 ||
				now >= animationTime
			) {
				break
			}
		}
	}
	private val radPerDeg = 6.283f / 360f
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

	private var thread: Thread? = null
	private var animationStart = 0L
	private var radians = 0.0
	private var radiansBegin = 0.0
	private var radiansChange = 0.0
	private var noWeights = false

	init {
		val res = context.resources
		val dp = res.displayMetrics.density

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

		frame = BitmapFactory.decodeResource(res, R.drawable.scale_frame)
		val frameWidth = frame.width
		frameHeight = frame.height
		frameMidX = (frameWidth * .5f).roundToInt().toFloat()
		frameAxis = (frameHeight * .4f).roundToInt().toFloat()

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

	fun setWeights(left: Int, right: Int) {
		if (left < 0 || right < 0) {
			noWeights = true
			radians = 0.0
			invalidate()
		} else {
			noWeights = false
			radiansBegin = radians
			radiansChange = calculateBalance(
				left.toFloat(),
				right.toFloat()
			) - radiansBegin
			startAnimation()
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
		var result: Int

		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize
		} else {
			result = desiredSize
			if (specMode == MeasureSpec.AT_MOST) {
				result = min(result, specSize)
			}
		}

		return result
	}

	override fun onDraw(canvas: Canvas) {
		drawScale(canvas)
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		stopAnimation()
	}

	private fun stopAnimation() {
		if (thread != null) {
			thread?.interrupt()
			try {
				thread?.join()
			} catch (e: InterruptedException) {
				// parent thread was interrupted
			}
			thread = null
		}
	}

	private fun startAnimation() {
		if (thread?.isAlive != null) {
			stopAnimation()
		}
		animationStart = System.currentTimeMillis()
		thread = Thread(animationRunnable)
		thread?.start()
	}

	private fun drawScale(canvas: Canvas) {
		canvas.drawColor(backgroundColor)

		val centerX = (canvas.width / 2f).roundToInt().toFloat()
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
		pnt.color = noColor and 0xffffff or alphaMod
		canvas.drawText(
			noString,
			centerX - frameMidX - pnt.measureText(noString),
			top + textPad,
			pnt
		)
		pnt.color = maybeColor and 0xffffff or alphaMod
		canvas.drawText(maybeString, centerX, top - textPad * .5f, pnt)
		pnt.color = yesColor and 0xffffff or alphaMod
		canvas.drawText(yesString, centerX + frameMidX, top + textPad, pnt)

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
		mat.postRotate(radians.toFloat() / radPerDeg, centerX, topAxis)
		canvas.drawBitmap(scale, mat, pnt)
	}
}

private fun calculateBalance(left: Float, right: Float): Double {
	val min: Float = max(1f, min(left, right))
	val balance: Float = right - left
	var factor: Float = when {
		balance == 0f -> 0f
		balance > 0f -> min(min, balance)
		else -> max(-min, balance)
	}
	factor /= min
	return (.9f * factor).toDouble()
}

private fun linear(
	time: Long,
	begin: Double,
	change: Double,
	duration: Long
): Double {
	return change.toFloat() * time / duration + begin
}
