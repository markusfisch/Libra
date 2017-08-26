package de.markusfisch.android.libra.adapter

import de.markusfisch.android.libra.app.Recommendation
import de.markusfisch.android.libra.database.DataSource
import de.markusfisch.android.libra.R

import android.content.Context
import android.database.Cursor
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView

import java.util.Date

class IssuesAdapter(context: Context, cursor: Cursor):
		CursorAdapter(context, cursor, false) {
	private val dateFormat = DateFormat.getLongDateFormat(context)
	private val res = context.getResources()
	private val nameIndex = cursor.getColumnIndex(
			DataSource.ISSUES_NAME)
	private val createdIndex = cursor.getColumnIndex(
			DataSource.ISSUES_CREATED_TIMESTAMP)
	private val negativeIndex = cursor.getColumnIndex(
			DataSource.ISSUES_NEGATIVE)
	private val positiveIndex = cursor.getColumnIndex(
			DataSource.ISSUES_POSITIVE)

	override fun newView(
			context: Context,
			cursor: Cursor,
			parent: ViewGroup): View  {
		return LayoutInflater.from(parent.getContext()).inflate(
				R.layout.item_issue, parent, false)
	}

	override fun bindView(
			view: View,
			context: Context,
			cursor: Cursor) {
		val holder = getViewHolder(view)
		val icon: Int = when (Recommendation.getRecommendation(
				cursor.getInt(negativeIndex),
				cursor.getInt(positiveIndex))) {
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
		holder.nameView.setText(name)
		holder.createdView.setText(getHumanTime(time))
	}

	private fun getViewHolder(view: View): ViewHolder {
		var holder = view.getTag() as ViewHolder?
		if (holder == null) {
			holder = ViewHolder(
					view.findViewById(R.id.icon),
					view.findViewById(R.id.name),
					view.findViewById(R.id.created))
			view.setTag(holder)
		}
		return holder
	}

	private fun getHumanTime(time: Long): String {
		val since = System.currentTimeMillis() / 1000L - time
		val hour = 3600
		val day = hour * 24

		if (since < 60L) {
			return res.getString(R.string.just_now)
		} else if (since < hour) {
			val minutes = since.toInt() / 60
			return res.getQuantityString(R.plurals.minutes_ago,
					minutes,
					minutes)
		} else if (since < hour * 24L) {
			val hours = (since / hour).toInt()
			return res.getQuantityString(R.plurals.hours_ago,
					hours,
					hours)
		} else if (since < hour * 72L) {
			val days = (since / day).toInt()
			return res.getQuantityString(R.plurals.days_ago,
					days,
					days)
		}

		return dateFormat.format(Date(time * 1000L))
	}

	private data class ViewHolder(
			val iconView: ImageView,
			val nameView: TextView,
			val createdView: TextView)
}
