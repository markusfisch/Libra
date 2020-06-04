package de.markusfisch.android.libra.app

import android.app.Application
import de.markusfisch.android.libra.database.Database
import de.markusfisch.android.libra.preferences.Preferences

val db = Database()
val prefs = Preferences()

class LibraApp : Application() {
	override fun onCreate() {
		super.onCreate()
		db.open(this)
		prefs.init(this)
	}
}

