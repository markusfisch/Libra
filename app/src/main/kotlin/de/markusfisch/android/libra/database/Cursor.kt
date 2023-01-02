package de.markusfisch.android.libra.database

import android.database.Cursor

// Overwrite Kotlin's ".use" function for Cursor because Cursor cannot
// be cast to Closeable below API level 16. This should be removed when
// the minSDK is increased to at least JELLY_BEAN (16).
inline fun <R> Cursor.use(block: (Cursor) -> R): R = try {
	block.invoke(this)
} finally {
	close()
}

fun Cursor.getString(name: String): String {
	val idx = getColumnIndex(name)
	return if (idx < 0) "" else getString(idx) ?: ""
}

fun Cursor.getInt(name: String): Int {
	val idx = getColumnIndex(name)
	return if (idx < 0) 0 else getInt(idx)
}
