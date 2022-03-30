package de.markusfisch.android.libra.database

import android.database.Cursor

fun Cursor.getString(name: String): String {
	val idx = getColumnIndex(name)
	return if (idx < 0) "" else getString(idx) ?: ""
}

fun Cursor.getInt(name: String): Int {
	val idx = getColumnIndex(name)
	return if (idx < 0) 0 else getInt(idx)
}
