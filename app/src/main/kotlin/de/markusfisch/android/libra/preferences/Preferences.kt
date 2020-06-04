package de.markusfisch.android.libra.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate

class Preferences {
	lateinit var preferences: SharedPreferences

	var design: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		set(value) {
			// this needs to be written immediately because the app
			// may be about to restart
			commit(DESIGN, value)
			field = value
			AppCompatDelegate.setDefaultNightMode(value)
		}

	fun init(context: Context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context)
		update()
	}

	fun update() {
		design = preferences.getInt(DESIGN, design)
	}

	private fun commit(label: String, value: Int) =
		put(label, value).commit()

	private fun put(label: String, value: Int) =
		preferences.edit().putInt(label, value)

	companion object {
		const val DESIGN = "design"
	}
}
