package de.markusfisch.android.libra.app

import android.content.Context
import android.content.Intent

fun shareText(context: Context, text: String, type: String = "text/plain") {
	val intent = Intent(Intent.ACTION_SEND)
	intent.putExtra(Intent.EXTRA_TEXT, text)
	intent.type = type
	context.startActivity(intent)
}
