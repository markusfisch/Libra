package de.markusfisch.android.libra.app

import de.markusfisch.android.libra.database.Database

import android.app.Application

val db = Database()

class LibraApp : Application() {
	override fun onCreate() {
		super.onCreate()
		db.open(this)
	}
}
