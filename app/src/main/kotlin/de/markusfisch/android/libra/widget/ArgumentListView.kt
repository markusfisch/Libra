package de.markusfisch.android.libra.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ListView

class ArgumentListView: ListView {
	var isWeighing = false
			private set

	private enum class Mode {
		NONE,
		SCROLLING,
		WEIGHING
	}

	private val rect = Rect()
	private val touchSlop: Int

	private var downX: Float = 0f
	private var downY: Float = 0f
	private var downWeight: Int? = 0
	private var argumentView: ArgumentView? = null
	private var mode = Mode.NONE

	constructor(context: Context, attrs: AttributeSet, defStyle: Int):
			super(context, attrs, defStyle) {
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop()
	}

	constructor(context: Context, attrs: AttributeSet):
			this(context, attrs, 0) {}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.getActionMasked()) {
			MotionEvent.ACTION_DOWN -> {
				downX = event.getX()
				downY = event.getY()
				argumentView = findView(event)
				argumentView?.onTouchDown(event)
				downWeight = argumentView?.weight
				mode = Mode.NONE
			}
			MotionEvent.ACTION_MOVE -> {
				val dx = Math.abs(downX - event.getX())
				val dy = Math.abs(downY - event.getY())
				if (mode == Mode.NONE && dx + dy >= touchSlop) {
					if (dx > dy) {
						mode = Mode.WEIGHING
					} else {
						mode = Mode.SCROLLING
					}
				}
				if (mode == Mode.WEIGHING) {
					argumentView?.onTouchMove(event)
					isWeighing = downWeight != argumentView?.weight
					return true
				}
			}
			MotionEvent.ACTION_UP -> {
				argumentView?.onTouchUp()
				isWeighing = false
			}
			MotionEvent.ACTION_CANCEL -> {
				argumentView?.onTouchCancel()
				isWeighing = false
			}
		}
		return super.onTouchEvent(event)
	}

	private fun findView(event: MotionEvent): ArgumentView? {
		val listViewCoords = IntArray(2)
		getLocationOnScreen(listViewCoords)
		val x = event.getRawX().toInt() - listViewCoords[0]
		val y = event.getRawY().toInt() - listViewCoords[1]
		val childCount = getChildCount() - 1
		for (it in 0..childCount) {
			var child = getChildAt(it)
			child?.getHitRect(rect)
			if (child is ArgumentView && rect.contains(x, y)) {
				return child
			}
		}
		return null
	}
}
