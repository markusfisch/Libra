package de.markusfisch.android.libra.app

import de.markusfisch.android.libra.database.DataSource

import android.app.Application

class LibraApp: Application() {
	companion object {
		val data = DataSource()
	}

	override fun onCreate() {
		super.onCreate()
		data.open(this)
	}
}
