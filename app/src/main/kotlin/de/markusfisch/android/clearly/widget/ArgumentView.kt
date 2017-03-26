package de.markusfisch.android.clearly.widget

import de.markusfisch.android.clearly.app.ClearlyApp
import de.markusfisch.android.clearly.fragment.ArgumentsFragment
import de.markusfisch.android.clearly.R

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView

class ArgumentView: TextView {
	var id: Long = 0
	var weight: Int = 0

	private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val padding: Float

	private var width: Float = 0f
	private var height: Float = 0f
	private var block: Float = 0f
	private var center: Float = 0f
	private var savedX: Float = -1f
	private var savedWeight: Int = 0

	constructor(context: Context, attrs: AttributeSet, defStyle: Int):
			super(context, attrs, defStyle) {
		setOnTouchListener { v, event ->
			when (event.getActionMasked()) {
				MotionEvent.ACTION_DOWN -> {
					savedX = event.getX()
					savedWeight = weight
					true
				}
				MotionEvent.ACTION_MOVE -> {
					val mod = Math.round((event.getX() - savedX) * 2f / block)
					weight = Math.max(-10, Math.min(10, savedWeight + mod))
					invalidate()
					true
				}
				MotionEvent.ACTION_UP -> {
					if (weight == savedWeight) {
						editText()
					} else {
						storeWeight()
					}
					savedX = -1f
					performClick()
					invalidate()
					true
				}
				MotionEvent.ACTION_CANCEL -> {
					weight = savedWeight
					savedX = -1f
					invalidate()
					true
				}
				else -> false
			}
		}
		val dp = context.getResources().getDisplayMetrics().density
		padding = dp * 4
	}

	constructor(context: Context, attrs: AttributeSet):
			this(context, attrs, 0) {}

	public override fun onLayout(
			changed: Boolean,
			left: Int,
			top: Int,
			right: Int,
			bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		width = (right - left).toFloat()
		height = (bottom - top).toFloat()
		center = Math.round(width / 2f).toFloat()
		block = Math.round(center / 10f).toFloat()
	}

	public override fun onDraw(canvas: Canvas) {
		if (savedX > -1f) {
			paint.setColor(0xffdfdfdf.toInt())
			canvas.drawRect(0f, 0f, width, height, paint)
		}

		var top = height - padding * 3f
		var bottom = height - padding * 2f
		paint.setColor(0xfff0f0f0.toInt())
		canvas.drawRect(
				padding,
				top,
				width - padding,
				bottom,
				paint)

		var x: Float
		var step: Float
		var color: Int
		if (weight > 0) {
			x = center
			step = block
			color = 0xff55d400.toInt()
		} else {
			x = center - block + padding
			step = -block
			color = 0xffff6600.toInt()
		}
		paint.setColor(color)
		var i = Math.abs(weight % 11)
		while (i-- > 0) {
			canvas.drawRect(
					x,
					top,
					x + block - padding,
					bottom,
					paint)
			x += step
		}

		super.onDraw(canvas)
	}

	private fun editText() {
		getArgumentsFragment()?.editArgument(id)
	}

	private fun storeWeight() {
		ClearlyApp.data.updateArgumentWeight(id, weight)
		getArgumentsFragment()?.reloadList()
	}

	private fun getArgumentsFragment(): ArgumentsFragment? {
		val activity = getContext()
		if (activity is AppCompatActivity) {
			val fragment = activity.getSupportFragmentManager()
					.findFragmentById(R.id.content_frame)
			if (fragment is ArgumentsFragment) {
				return fragment
			}
		}
		return null
	}
}
