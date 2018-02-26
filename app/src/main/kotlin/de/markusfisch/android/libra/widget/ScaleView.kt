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
import android.view.SurfaceHolder
import android.view.SurfaceView

class ScaleView(context: Context) : SurfaceView(context) {
	private val animationTime = 300L
	private val animationRunnable: Runnable = Runnable {
		while (running) {
			val now = Math.min(
				System.currentTimeMillis() - animationStart,
				animationTime
			)

			radians = linear(
				now,
				radiansBegin,
				radiansChange,
				animationTime
			)

			lockCanvasAndDraw()

			if (now >= animationTime) {
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

	private var width = 0f
	private var height = 0f
	private var running = false
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
		topMargin = Math.round(32f * dp)
		bottomMargin = Math.round(8f * dp)

		// toInt() is required or Kotlin thinks it's a Long
		transparentColor = 0x40000000.toInt()
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
		frameMidX = Math.round(frameWidth * .5f).toFloat()
		frameAxis = Math.round(frameHeight * .4f).toFloat()

		scale = BitmapFactory.decodeResource(res, R.drawable.scale_bar)
		val scaleWidth = scale.width
		val scaleHeight = scale.height
		scaleMidX = Math.round(scaleWidth * .5f).toFloat()
		scaleMidY = Math.round(scaleHeight * .5f).toFloat()
		scaleRadius = Math.round(scaleWidth * .48f).toFloat()

		pan = BitmapFactory.decodeResource(res, R.drawable.scale_pan)
		val panWidth = pan.width
		panMidX = Math.round(panWidth * .5f).toFloat()

		initSurfaceHolder()
	}

	fun setWeights(left: Int, right: Int) {
		if (left < 0 || right < 0) {
			noWeights = true
			radians = 0.0
			lockCanvasAndDraw()
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
				result = Math.min(result, specSize)
			}
		}

		return result
	}

	private fun initSurfaceHolder() {
		holder.setFormat(PixelFormat.TRANSPARENT)
		holder.addCallback(object : SurfaceHolder.Callback {
			override fun surfaceChanged(
				holder: SurfaceHolder,
				format: Int,
				width: Int,
				height: Int
			) {
				this@ScaleView.width = width.toFloat()
				this@ScaleView.height = height.toFloat()
				lockCanvasAndDraw()
			}

			override fun surfaceCreated(holder: SurfaceHolder) {
			}

			override fun surfaceDestroyed(holder: SurfaceHolder) {
				stopAnimation()
			}
		})
	}

	private fun stopAnimation() {
		running = false
		for (it in 0..100) {
			try {
				thread?.join()
				thread = null
				break
			} catch (e: InterruptedException) {
				// try again
			}
		}
	}

	private fun startAnimation() {
		if (width > 0) {
			if (running) {
				stopAnimation()
			}
			running = true
			animationStart = System.currentTimeMillis()
			thread = Thread(animationRunnable)
			thread?.start()
		} else {
			postDelayed({ startAnimation() }, 100)
		}
	}

	private fun lockCanvasAndDraw() {
		if (holder.surface.isValid) {
			val canvas = holder.lockCanvas()
			if (canvas != null) {
				drawScale(canvas)
				holder.unlockCanvasAndPost(canvas)
			}
		}
	}

	private fun drawScale(canvas: Canvas) {
		canvas.drawColor(backgroundColor)

		val centerX = Math.round(width / 2f).toFloat()
		val top = topMargin.toFloat()

		val alphaMod = if (noWeights) {
			pnt.color = transparentColor
			transparentColor
		} else {
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
		val rx = centerX + scaleRadius * Math.cos(radians).toFloat()
		val ry = topAxis + scaleRadius * Math.sin(radians).toFloat()
		val lx = centerX + scaleRadius * Math.cos(radians + Math.PI).toFloat()
		val ly = topAxis + scaleRadius * Math.sin(radians + Math.PI).toFloat()

		mat.setTranslate(lx - panMidX, ly)
		canvas.drawBitmap(pan, mat, pnt)

		mat.setTranslate(rx - panMidX, ry)
		canvas.drawBitmap(pan, mat, pnt)

		mat.setTranslate(centerX - scaleMidX, topAxis - scaleMidY)
		mat.postRotate(radians.toFloat() / radPerDeg, centerX, topAxis)
		canvas.drawBitmap(scale, mat, pnt)
	}

	companion object {
		private fun calculateBalance(left: Float, right: Float): Double {
			val min: Float = Math.max(1f, Math.min(left, right))
			val balance: Float = right - left
			var factor: Float
			factor = if (balance == 0f) {
				0f
			} else if (balance > 0f) {
				Math.min(min, balance)
			} else {
				Math.max(-min, balance)
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
	}
}
