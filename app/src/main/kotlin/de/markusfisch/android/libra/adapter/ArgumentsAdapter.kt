package de.markusfisch.android.libra.adapter

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.database.Database
import de.markusfisch.android.libra.widget.ArgumentView

class ArgumentsAdapter(context: Context, cursor: Cursor) :
	CursorAdapter(context, cursor, false) {
	private val idIndex = cursor.getColumnIndex(
		Database.ARGUMENTS_ID
	)
	private val textIndex = cursor.getColumnIndex(
		Database.ARGUMENTS_TEXT
	)
	private val weightIndex = cursor.getColumnIndex(
		Database.ARGUMENTS_WEIGHT
	)

	var selectedId = 0L

	override fun newView(
		context: Context,
		cursor: Cursor,
		parent: ViewGroup
	): View {
		return LayoutInflater.from(parent.context).inflate(
			R.layout.item_argument, parent, false
		)
	}

	override fun bindView(
		view: View,
		context: Context,
		cursor: Cursor
	) {
		val itemId = cursor.getLong(idIndex)
		getViewHolder(view).argumentView.apply {
			id = itemId
			text = cursor.getString(textIndex)
			weight = cursor.getInt(weightIndex)
		}
		val selected = itemId == selectedId
		view.post {
			view.isSelected = selected
		}
	}

	private fun getViewHolder(view: View): ViewHolder {
		var holder = view.tag as ViewHolder?
		if (holder == null) {
			holder = ViewHolder(view.findViewById(R.id.text))
			view.tag = holder
		}
		return holder
	}

	private data class ViewHolder(val argumentView: ArgumentView)
}
