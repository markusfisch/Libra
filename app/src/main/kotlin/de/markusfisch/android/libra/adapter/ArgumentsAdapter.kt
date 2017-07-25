package de.markusfisch.android.libra.adapter

import de.markusfisch.android.libra.database.DataSource
import de.markusfisch.android.libra.widget.ArgumentView
import de.markusfisch.android.libra.R

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter

class ArgumentsAdapter(context: Context, cursor: Cursor):
		CursorAdapter(context, cursor, false) {
	private val idIndex = cursor.getColumnIndex(
			DataSource.ARGUMENTS_ID)
	private val textIndex = cursor.getColumnIndex(
			DataSource.ARGUMENTS_TEXT)
	private val weightIndex = cursor.getColumnIndex(
			DataSource.ARGUMENTS_WEIGHT)

	override fun newView(
			context: Context,
			cursor: Cursor,
			parent: ViewGroup): View  {
		return LayoutInflater.from(parent.context).inflate(
				R.layout.item_argument, parent, false)
	}

	override fun bindView(
			view: View,
			context: Context,
			cursor: Cursor) {
		val holder = getViewHolder(view)
		holder.argumentView.id = cursor.getLong(idIndex)
		holder.argumentView.text = cursor.getString(textIndex)
		holder.argumentView.weight = cursor.getInt(weightIndex)
	}

	private fun getViewHolder(view: View): ViewHolder {
		var holder = view.tag as ViewHolder?
		if (holder == null) {
			holder = ViewHolder(view.findViewById(R.id.text) as ArgumentView)
			view.tag = holder
		}
		return holder
	}

	private data class ViewHolder(val argumentView: ArgumentView)
}
