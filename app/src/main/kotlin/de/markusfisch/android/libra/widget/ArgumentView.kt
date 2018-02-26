package de.markusfisch.android.libra.widget

import de.markusfisch.android.libra.app.LibraApp
import de.markusfisch.android.libra.fragment.ArgumentsFragment
import de.markusfisch.android.libra.R

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v7.app.AppCompatActivity
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.TextView

class ArgumentView : TextView {
	var id: Long = 0
	var weight: Int = 0
		set(value) {
			gravity = when {
				value < 0 -> Gravity.LEFT
				value > 0 -> Gravity.RIGHT
				else -> Gravity.CENTER_HORIZONTAL
			}
			field = value
		}

	private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val padding: Float
	private val positiveColor: Int
	private val negativeColor: Int
	private val barColor: Int
	private val swipeLeft: Bitmap
	private val swipeRight: Bitmap
	private val swipeWidth: Int
	private val swipeHeight: Int

	private var width: Float = 0f
	private var height: Float = 0f
	private var block: Float = 0f
	private var center: Float = 0f
	private var savedX: Float = -1f
	private var savedWeight: Int = 0

	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle) {
		val res = context.resources
		val dp = res.displayMetrics.density
		padding = dp * 4
		positiveColor = ContextCompat.getColor(context, R.color.yes)
		negativeColor = ContextCompat.getColor(context, R.color.no)
		barColor = ContextCompat.getColor(context, R.color.weight_bar)
		swipeLeft = BitmapFactory.decodeResource(res, R.drawable.swipe_left)
		swipeRight = BitmapFactory.decodeResource(res, R.drawable.swipe_right)
		swipeWidth = swipeLeft.width
		swipeHeight = swipeLeft.height
	}

	constructor(context: Context, attrs: AttributeSet) :
			this(context, attrs, 0)

	fun onTouchDown(event: MotionEvent) {
		savedX = event.x
		savedWeight = weight
	}

	fun onTouchMove(event: MotionEvent) {
		val mod = Math.round((event.x - savedX) * 2f / block)
		weight = Math.max(-10, Math.min(10, savedWeight + mod))
		invalidate()
	}

	fun onTouchUp() {
		if (weight != savedWeight) {
			storeWeight()
		}
		invalidate()
	}

	fun onTouchCancel() {
		weight = savedWeight
		invalidate()
	}

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
		center = Math.round(width / 2f).toFloat()
		block = Math.round(center / 10f).toFloat()
	}

	override fun onDraw(canvas: Canvas) {
		val top = height - padding * 3f
		val bottom = height - padding * 2f
		paint.color = barColor
		canvas.drawRect(
			padding,
			top,
			width - padding,
			bottom,
			paint
		)

		if (weight == 0) {
			drawArrows(canvas)
		} else {
			drawWeightBar(canvas, top, bottom)
		}

		super.onDraw(canvas)
	}

	private fun drawArrows(canvas: Canvas) {
		val pad = padding * 4
		val y = (height - swipeHeight) / 2
		val right = width - swipeWidth - pad
		canvas.drawBitmap(swipeLeft, pad, y, null)
		canvas.drawBitmap(swipeRight, right, y, null)
	}

	private fun drawWeightBar(canvas: Canvas, top: Float, bottom: Float) {
		var x: Float
		val step: Float
		val color: Int
		if (weight > 0) {
			x = center
			step = block
			color = positiveColor
		} else {
			x = center - block + padding
			step = -block
			color = negativeColor
		}

		paint.color = color
		for (it in 1..Math.abs(weight % 11)) {
			canvas.drawRect(
				x,
				top,
				x + block - padding,
				bottom,
				paint
			)
			x += step
		}
	}

	private fun storeWeight() {
		LibraApp.data.updateArgumentWeight(id, weight)
		getArgumentsFragment()?.reloadList()
	}

	private fun getArgumentsFragment(): ArgumentsFragment? {
		val activity = context
		if (activity is AppCompatActivity) {
			val fragment = activity.supportFragmentManager
				.findFragmentById(R.id.content_frame)
			if (fragment is ArgumentsFragment) {
				return fragment
			}
		}
		return null
	}
}
