package de.markusfisch.android.clearly.widget

import de.markusfisch.android.clearly.R

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView

class RecommendationView(context: Context): TextView(context) {
	var leftWeight: Int = 0
	var rightWeight: Int = 0

	private val mat = Matrix()
	private val padding: Float
	private val frame: Bitmap
	private val frameMidX: Float
	private val frameMidY: Float
	private val frameAxis: Float
	private val scale: Bitmap
	private val scaleMidX: Float
	private val scaleMidY: Float
	private val scaleRadius: Float
	private val pan: Bitmap
	private val panMidX: Float

	private var width: Float = 0f
	private var height: Float = 0f

	init {
		val res = context.getResources()
		val dp = res.getDisplayMetrics().density
		padding = 16f * dp
		val p = padding.toInt()
		setPadding(p, p, p, p)
		setGravity(Gravity.BOTTOM)

		frame = BitmapFactory.decodeResource(res, R.drawable.ic_frame)
		val frameWidth = frame.getWidth()
		val frameHeight = frame.getHeight()
		frameMidX = Math.round(frameWidth * .5f).toFloat()
		frameMidY = Math.round(frameHeight * .5f).toFloat()
		frameAxis = Math.round(frameHeight * .56f).toFloat()

		scale = BitmapFactory.decodeResource(res, R.drawable.ic_scale)
		val scaleWidth = scale.getWidth()
		val scaleHeight = scale.getHeight()
		scaleMidX = Math.round(scaleWidth * .5f).toFloat()
		scaleMidY = Math.round(scaleHeight * .5f).toFloat()
		scaleRadius = Math.round(scaleWidth * .48f).toFloat()

		pan = BitmapFactory.decodeResource(res, R.drawable.ic_pan)
		val panWidth = pan.getWidth()
		panMidX = Math.round(panWidth * .5f).toFloat()
	}

	public fun setWeight(left: Int, right: Int) {
		leftWeight = left
		rightWeight = right
	}

	public override fun onMeasure(widthSpec: Int, heightSpec: Int) {
		setMeasuredDimension(
				widthSpec,
				Math.round(frame.getHeight() + padding * 2))
	}

	public override fun onLayout(
			changed: Boolean,
			left: Int,
			top: Int,
			right: Int,
			bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		width = (right - left).toFloat()
		height = (bottom - top).toFloat()
	}

	public override fun onDraw(canvas: Canvas) {
		canvas.drawColor(0)

		val cx = Math.round(width / 2f).toFloat()

		mat.setTranslate(cx - frameMidX,
				Math.round(height / 2f).toFloat() - frameMidY)
		canvas.drawBitmap(frame, mat, null)

		val balance: Float = (rightWeight - leftWeight).toFloat()
		val min: Float = Math.max(1f, Math.min(
				leftWeight.toFloat(),
				rightWeight.toFloat()))
		var factor: Float
		if (balance == 0f) {
			factor = 0f
		} else if (balance > 0f) {
			factor = Math.min(min, balance)
		} else {
			factor = Math.max(-min, balance)
		}
		factor /= min
		val lock = .9f
		val radians: Double = (lock * factor).toDouble()
		val degrees: Float = lock / 6.283f * 360f * factor

		val rx = cx + scaleRadius * Math.cos(radians).toFloat()
		val ry = frameAxis + scaleRadius * Math.sin(radians).toFloat()
		val lx = cx + scaleRadius * Math.cos(radians + Math.PI).toFloat()
		val ly = frameAxis +
				scaleRadius * Math.sin(radians + Math.PI).toFloat()

		mat.setTranslate(lx - panMidX, ly)
		canvas.drawBitmap(pan, mat, null)

		mat.setTranslate(rx - panMidX, ry)
		canvas.drawBitmap(pan, mat, null)

		mat.setTranslate(cx - scaleMidX, frameAxis - scaleMidY)
		mat.postRotate(degrees, cx, frameAxis)
		canvas.drawBitmap(scale, mat, null)

		//super.onDraw(canvas)
	}
}
