package de.markusfisch.android.libra.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ListView

class ArgumentListView: ListView {
	var isWeighing = false
			private set

	private val rect = Rect()

	private var downX: Float = 0f
	private var downY: Float = 0f
	private var swipeView: ArgumentView? = null

	constructor(context: Context, attrs: AttributeSet, defStyle: Int):
			super(context, attrs, defStyle) {
	}

	constructor(context: Context, attrs: AttributeSet):
			this(context, attrs, 0) {}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.getActionMasked()) {
			MotionEvent.ACTION_DOWN -> {
				downX = event.getX()
				downY = event.getY()
				swipeView = findView(event)
				swipeView?.onTouchDown(event)
			}
			MotionEvent.ACTION_MOVE -> {
				// don't route ACTION_MOVE to ListView if the
				// gesture is more horizontal than vertical
				if (Math.abs(downX - event.getX()) >
						Math.abs(downY - event.getY())) {
					swipeView?.onTouchMove(event)
					isWeighing = true
					return false
				}
			}
			MotionEvent.ACTION_UP -> {
				swipeView?.onTouchUp()
				isWeighing = false
			}
			MotionEvent.ACTION_CANCEL -> {
				swipeView?.onTouchCancel()
				isWeighing = false
			}
		}
		return super.onTouchEvent(event)
	}

	private fun findView(event: MotionEvent): ArgumentView? {
		val childCount = getChildCount()
		val listViewCoords = IntArray(2)
		getLocationOnScreen(listViewCoords)
		val x = event.getRawX().toInt() - listViewCoords[0]
		val y = event.getRawY().toInt() - listViewCoords[1]
		var child: View
		var i = 0
		while (i < childCount) {
			child = getChildAt(i)
			child.getHitRect(rect)
			if (child is ArgumentView && rect.contains(x, y)) {
				return child
			}
			++i
		}
		return null
	}
}
