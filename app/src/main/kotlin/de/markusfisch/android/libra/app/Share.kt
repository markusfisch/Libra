package de.markusfisch.android.libra.app

import android.content.Context
import android.content.Intent

fun Context.shareText(text: String, mimeType: String = "text/plain") {
	startActivity(Intent(Intent.ACTION_SEND).apply {
		putExtra(Intent.EXTRA_TEXT, text)
		type = mimeType
	})
}
