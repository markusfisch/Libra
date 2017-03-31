package de.markusfisch.android.libra.widget

import de.markusfisch.android.libra.R

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.widget.TextView

class ScaleView(context: Context): TextView(context) {
	var leftWeight: Int = 0
	var rightWeight: Int = 0

	private val radPerDeg = 6.283f / 360f
	private val pnt = Paint(Paint.ANTI_ALIAS_FLAG)
	private val mat = Matrix()
	private val padding: Int
	private val transparentColor: Int
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

	private var textHeight: Int = 0
	private var width: Float = 0f
	private var height: Float = 0f

	init {
		val res = context.getResources()
		val dp = res.getDisplayMetrics().density

		pnt.setTextSize(12f * dp)
		padding = Math.round(16f * dp)

		transparentColor = 0x40000000.toInt()
		yesColor = ContextCompat.getColor(context, R.color.yes)
		yesString = context.getString(R.string.yes)
		maybeColor = ContextCompat.getColor(context, R.color.maybe)
		maybeString = context.getString(R.string.maybe)
		noColor = ContextCompat.getColor(context, R.color.no)
		noString = context.getString(R.string.no)

		setPadding(padding, padding, padding, padding)
		setGravity(Gravity.CENTER_HORIZONTAL)

		frame = BitmapFactory.decodeResource(res, R.drawable.scale_frame)
		val frameWidth = frame.getWidth()
		frameHeight = frame.getHeight()
		frameMidX = Math.round(frameWidth * .5f).toFloat()
		frameAxis = Math.round(frameHeight * .4f).toFloat()

		scale = BitmapFactory.decodeResource(res, R.drawable.scale_bar)
		val scaleWidth = scale.getWidth()
		val scaleHeight = scale.getHeight()
		scaleMidX = Math.round(scaleWidth * .5f).toFloat()
		scaleMidY = Math.round(scaleHeight * .5f).toFloat()
		scaleRadius = Math.round(scaleWidth * .48f).toFloat()

		pan = BitmapFactory.decodeResource(res, R.drawable.scale_pan)
		val panWidth = pan.getWidth()
		panMidX = Math.round(panWidth * .5f).toFloat()
	}

	fun setWeights(left: Int, right: Int) {
		leftWeight = left
		rightWeight = right
	}

	override fun onMeasure(widthSpec: Int, heightSpec: Int) {
		val paint = getPaint()
		val bounds = Rect()
		val text = getText().toString()
		paint.getTextBounds(text, 0, text.length, bounds)
		textHeight = bounds.height()

		setMeasuredDimension(
				widthSpec,
				padding * 2 + textHeight + frameHeight)
	}

	override fun onLayout(
			changed: Boolean,
			left: Int,
			top: Int,
			right: Int,
			bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		width = (right - left).toFloat()
		height = (bottom - top).toFloat()
	}

	override fun onDraw(canvas: Canvas) {
		canvas.drawColor(0)

		val centerX = Math.round(width / 2f).toFloat()
		val top = (padding * 2 + textHeight).toFloat()

		var bitmapPaint: Paint? = null
		var alphaMod = 0xff000000.toInt()
		val radians: Double = if (leftWeight > -1) {
			// It's important to use a null paint for drawBitmap()
			// when drawing transparent PNG's with no alpha value.
			// Using 0xffffffff gives jagged edges because that's
			// multiplied with the PNG's alpha channel.
			calculateBalance()
		} else {
			pnt.setColor(transparentColor)
			alphaMod = transparentColor
			bitmapPaint = pnt
			0.0
		}

		pnt.setColor(noColor and 0xffffff or alphaMod)
		canvas.drawText(noString,
				centerX - frameMidX - pnt.measureText(noString),
				top + padding,
				pnt)
		pnt.setColor(maybeColor and 0xffffff or alphaMod)
		canvas.drawText(maybeString, centerX, top - padding * .5f, pnt)
		pnt.setColor(yesColor and 0xffffff or alphaMod)
		canvas.drawText(yesString, centerX + frameMidX, top + padding, pnt)

		mat.setTranslate(centerX - frameMidX, top)
		canvas.drawBitmap(frame, mat, bitmapPaint)

		val topAxis = top + frameAxis
		val rx = centerX + scaleRadius * Math.cos(radians).toFloat()
		val ry = topAxis + scaleRadius * Math.sin(radians).toFloat()
		val lx = centerX + scaleRadius * Math.cos(radians + Math.PI).toFloat()
		val ly = topAxis + scaleRadius * Math.sin(radians + Math.PI).toFloat()

		mat.setTranslate(lx - panMidX, ly)
		canvas.drawBitmap(pan, mat, bitmapPaint)

		mat.setTranslate(rx - panMidX, ry)
		canvas.drawBitmap(pan, mat, bitmapPaint)

		mat.setTranslate(centerX - scaleMidX, topAxis - scaleMidY)
		mat.postRotate(radians.toFloat() / radPerDeg, centerX, topAxis)
		canvas.drawBitmap(scale, mat, bitmapPaint)

		//super.onDraw(canvas)
	}

	private fun calculateBalance(): Double {
		val min: Float = Math.max(1f, Math.min(
				leftWeight.toFloat(),
				rightWeight.toFloat()))
		val balance: Float = (rightWeight - leftWeight).toFloat()
		var factor: Float
		if (balance == 0f) {
			factor = 0f
		} else if (balance > 0f) {
			factor = Math.min(min, balance)
		} else {
			factor = Math.max(-min, balance)
		}
		factor /= min
		return (.9f * factor).toDouble()
	}
}
