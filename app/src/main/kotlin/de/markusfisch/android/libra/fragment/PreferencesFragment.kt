package de.markusfisch.android.libra.fragment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceGroup
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.activity.MainActivity
import de.markusfisch.android.libra.app.prefs
import de.markusfisch.android.libra.preferences.Preferences

class PreferencesFragment : PreferenceFragmentCompat() {
	private val changeListener = object : OnSharedPreferenceChangeListener {
		override fun onSharedPreferenceChanged(
			sharedPreferences: SharedPreferences,
			key: String
		) {
			val preference = findPreference(key) ?: return
			val design = prefs.design
			prefs.update()
			setSummary(preference)
			if (Preferences.DESIGN == key && design != prefs.design) {
				// setDefaultNightMode() in AppCompat 1.1.0 will automatically
				// update the app but since I want to keep the minSdk, I need
				// to restart the whole app to make sure the night mode setting
				// takes effect. Simply recreating the Activity doesn't work
				// when returning to MODE_NIGHT_FOLLOW_SYSTEM.
				restartApp(activity)
			}
		}
	}

	override fun onCreatePreferences(state: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.preferences)
		activity?.setTitle(R.string.preferences)
	}

	override fun onResume() {
		super.onResume()
		preferenceScreen
			.sharedPreferences
			.registerOnSharedPreferenceChangeListener(changeListener)
		setSummaries(preferenceScreen)
	}

	override fun onPause() {
		super.onPause()
		preferenceScreen
			.sharedPreferences
			.unregisterOnSharedPreferenceChangeListener(changeListener)
	}

	private fun setSummary(preference: Preference) {
		if (preference is ListPreference) {
			preference.setSummary(preference.entry)
		} else if (preference is PreferenceGroup) {
			setSummaries(preference)
		}
	}

	private fun setSummaries(screen: PreferenceGroup) {
		var i = screen.preferenceCount
		while (i-- > 0) {
			setSummary(screen.getPreference(i))
		}
	}
}

fun restartApp(activity: Activity? = null) {
	if (activity != null) {
		val intent = Intent(activity, MainActivity::class.java)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		intent.putExtra(MainActivity.OPEN_PREFERENCES, true)
		activity.startActivity(intent)
		activity.finish()
	}
	Runtime.getRuntime().exit(0)
}
