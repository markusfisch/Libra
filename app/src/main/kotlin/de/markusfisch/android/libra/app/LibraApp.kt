package de.markusfisch.android.libra.app

import de.markusfisch.android.libra.database.DataSource

import android.app.Application

class LibraApp : Application() {
	override fun onCreate() {
		super.onCreate()
		data.open(this)
	}

	companion object {
		val data = DataSource()
	}
}
