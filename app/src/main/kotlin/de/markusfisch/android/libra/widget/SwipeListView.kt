package de.markusfisch.android.libra.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ListView

class SwipeListView: ListView {
	private var downX: Float = 0f
	private var downY: Float = 0f

	constructor(context: Context, attrs: AttributeSet, defStyle: Int):
			super(context, attrs, defStyle) {
	}

	constructor(context: Context, attrs: AttributeSet):
			this(context, attrs, 0) {}

	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		when (ev.getAction()) {
			MotionEvent.ACTION_DOWN -> {
				downX = ev.getX()
				downY = ev.getY()
			}
			MotionEvent.ACTION_MOVE -> {
				// don't route ACTION_MOVE to ListView if the
				// gesture is more horizontal than vertical
				if (Math.abs(downX - ev.getX()) >
						Math.abs(downY - ev.getY())) {
					return false
				}
			}
		}
		return super.onInterceptTouchEvent(ev)
	}
}
