package de.markusfisch.android.libra.app

import android.app.Application
import de.markusfisch.android.libra.database.Database

val db = Database()

class LibraApp : Application() {
	override fun onCreate() {
		super.onCreate()
		db.open(this)
	}
}
