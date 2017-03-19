package de.markusfisch.android.clearly.app

import de.markusfisch.android.clearly.database.DataSource

import android.app.Application

class ClearlyApp(): Application() {
	companion object {
		val data = DataSource()
	}

	override fun onCreate() {
		super.onCreate()
		data.open(this)
	}
}
