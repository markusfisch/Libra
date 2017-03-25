package de.markusfisch.android.clearly.adapter

import de.markusfisch.android.clearly.database.DataSource
import de.markusfisch.android.clearly.R

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView

class DecisionsAdapter(context: Context, cursor: Cursor):
		CursorAdapter(context, cursor, false) {
	private val nameIndex = cursor.getColumnIndex(
			DataSource.DECISIONS_NAME)
	private val createdIndex = cursor.getColumnIndex(
			DataSource.DECISIONS_CREATED_STRING)

	override fun newView(
			context: Context,
			cursor: Cursor,
			parent: ViewGroup): View  {
		return LayoutInflater.from(parent.getContext()).inflate(
				R.layout.item_decision, parent, false)
	}

	override fun bindView(
			view: View,
			context: Context,
			cursor: Cursor) {
		val holder = getViewHolder(view)
		var name: String? = cursor.getString(nameIndex)
		if (name == null || name.isEmpty()) {
			name = context.getString(R.string.unnamed)
		}
		holder.nameView.setText(name)
		holder.createdView.setText(cursor.getString(createdIndex))
	}

	private fun getViewHolder(view: View): ViewHolder {
		var holder = view.getTag() as ViewHolder?
		if (holder == null) {
			holder = ViewHolder(
					view.findViewById(R.id.name) as TextView,
					view.findViewById(R.id.created) as TextView)
			view.setTag(holder)
		}
		return holder
	}

	private data class ViewHolder(
			val nameView: TextView,
			val createdView: TextView)
}
