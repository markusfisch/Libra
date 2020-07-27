package de.markusfisch.android.libra.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate

class Preferences {
	lateinit var preferences: SharedPreferences

	// must be a String because ListPreference works with String values only
	var design = "${AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM}"
		set(value) {
			// values need to be written immediately because the app may be
			// about to restart when the night mode setting needs to change
			put(DESIGN, value).commit()
			field = value
			AppCompatDelegate.setDefaultNightMode(value.toInt())
		}
	var showSums = true
		set(value) {
			put(SHOW_SUMS, value).commit()
			field = value
		}
	var sortOnInsert = false
		set(value) {
			put(SORT_ON_INSERT, value).commit()
			field = value
		}

	fun init(context: Context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context)
		update()
	}

	fun update() {
		design = preferences.getString(
			DESIGN,
			// this *was* an Int preference before using ListPreference
			preferences.getInt("design", design.toInt()).toString()
		) ?: design
		showSums = preferences.getBoolean(SHOW_SUMS, showSums)
		sortOnInsert = preferences.getBoolean(SORT_ON_INSERT, sortOnInsert)
	}

	private fun put(label: String, value: String) =
		preferences.edit().putString(label, value)

	private fun put(label: String, value: Boolean) =
		preferences.edit().putBoolean(label, value)

	companion object {
		const val DESIGN = "design_string"
		const val SHOW_SUMS = "show_sums"
		const val SORT_ON_INSERT = "sort_on_insert"
	}
}
