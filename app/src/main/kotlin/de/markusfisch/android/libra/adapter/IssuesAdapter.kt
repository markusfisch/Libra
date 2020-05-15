package de.markusfisch.android.libra.adapter

import android.content.Context
import android.database.Cursor
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.app.Recommendation
import de.markusfisch.android.libra.database.Database
import java.util.*

class IssuesAdapter(context: Context, cursor: Cursor) :
	CursorAdapter(context, cursor, false) {
	private val dateFormat = DateFormat.getLongDateFormat(context)
	private val res = context.resources
	private val idIndex = cursor.getColumnIndex(
		Database.ISSUES_ID
	)
	private val nameIndex = cursor.getColumnIndex(
		Database.ISSUES_NAME
	)
	private val createdIndex = cursor.getColumnIndex(
		Database.ISSUES_CREATED_TIMESTAMP
	)
	private val negativeIndex = cursor.getColumnIndex(
		Database.ISSUES_NEGATIVE
	)
	private val positiveIndex = cursor.getColumnIndex(
		Database.ISSUES_POSITIVE
	)

	var selectedId = 0L
	var selectedPosition = -1

	fun select(id: Long, position: Int) {
		this.selectedId = id
		this.selectedPosition = position
	}

	fun clearSelection() {
		this.selectedId = 0L
		this.selectedPosition = -1
	}

	override fun newView(
		context: Context,
		cursor: Cursor,
		parent: ViewGroup
	): View {
		return LayoutInflater.from(parent.context).inflate(
			R.layout.item_issue, parent, false
		)
	}

	override fun bindView(
		view: View,
		context: Context,
		cursor: Cursor
	) {
		val holder = getViewHolder(view)
		val icon: Int = when (Recommendation.getRecommendation(
			cursor.getInt(negativeIndex),
			cursor.getInt(positiveIndex)
		)) {
			Recommendation.YES -> R.drawable.ic_issue_yes
			Recommendation.MAYBE -> R.drawable.ic_issue_maybe
			Recommendation.NO -> R.drawable.ic_issue_no
			else -> R.drawable.ic_issue_incomplete
		}
		holder.iconView.setImageResource(icon)
		val time = cursor.getLong(createdIndex)
		var name: String? = cursor.getString(nameIndex)
		if (name == null || name.isEmpty()) {
			name = dateFormat.format(Date(time * 1000L))
		}
		holder.nameView.text = name
		holder.createdView.text = getHumanTime(time)
		val selected = cursor.getLong(idIndex) == selectedId
		view.post {
			view.isSelected = selected
		}
	}

	private fun getViewHolder(view: View): ViewHolder {
		var holder = view.tag as ViewHolder?
		if (holder == null) {
			holder = ViewHolder(
				view.findViewById(R.id.icon),
				view.findViewById(R.id.name),
				view.findViewById(R.id.created)
			)
			view.tag = holder
		}
		return holder
	}

	private fun getHumanTime(time: Long): String {
		val since = System.currentTimeMillis() / 1000L - time
		val hour = 3600
		val day = hour * 24

		when {
			since < 60L -> return res.getString(R.string.just_now)
			since < hour -> {
				val minutes = since.toInt() / 60
				return res.getQuantityString(
					R.plurals.minutes_ago,
					minutes,
					minutes
				)
			}
			since < hour * 24L -> {
				val hours = (since / hour).toInt()
				return res.getQuantityString(
					R.plurals.hours_ago,
					hours,
					hours
				)
			}
			since < hour * 72L -> {
				val days = (since / day).toInt()
				return res.getQuantityString(
					R.plurals.days_ago,
					days,
					days
				)
			}
			else -> return dateFormat.format(Date(time * 1000L))
		}
	}

	private data class ViewHolder(
		val iconView: ImageView,
		val nameView: TextView,
		val createdView: TextView
	)
}
