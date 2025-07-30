package de.markusfisch.android.libra.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.activity.MainActivity
import de.markusfisch.android.libra.app.prefs
import de.markusfisch.android.libra.app.requestWritePermission
import de.markusfisch.android.libra.database.Database
import de.markusfisch.android.libra.database.exportDatabase
import de.markusfisch.android.libra.database.importDatabase
import de.markusfisch.android.libra.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreferencesFragment : PreferenceFragmentCompat() {
	private val job = SupervisorJob()
	private val scope = CoroutineScope(Dispatchers.Default + job)
	private val changeListener = object : OnSharedPreferenceChangeListener {
		override fun onSharedPreferenceChanged(
			sharedPreferences: SharedPreferences?,
			key: String?
		) {
			key ?: return
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
				activity?.restartApp()
			}
		}
	}

	override fun onActivityResult(
		requestCode: Int, resultCode: Int,
		resultData: Intent?
	) {
		if (requestCode == PICK_FILE_RESULT_CODE &&
			resultCode == Activity.RESULT_OK &&
			resultData != null
		) {
			val ctx = context ?: return
			Toast.makeText(
				ctx,
				ctx.importDatabase(resultData.data),
				Toast.LENGTH_LONG
			).show()
		}
	}

	override fun onCreatePreferences(state: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.preferences)
		activity?.setTitle(R.string.preferences)
		findPreference<Preference>("import_database")?.onPreferenceClickListener =
			Preference.OnPreferenceClickListener { _ ->
				val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
				// In theory, it should be "application/x-sqlite3"
				// or the newer "application/vnd.sqlite3" but
				// only "application/octet-stream" works.
				chooseFile.type = "application/octet-stream"
				startActivityForResult(
					Intent.createChooser(
						chooseFile,
						getString(R.string.import_database)
					),
					PICK_FILE_RESULT_CODE
				)
				true
			}
		findPreference<Preference>("export_database")?.onPreferenceClickListener =
			Preference.OnPreferenceClickListener { _ ->
				activity?.askToExportToFile()
				true
			}
	}

	override fun onResume() {
		super.onResume()
		preferenceScreen
			.sharedPreferences
			?.registerOnSharedPreferenceChangeListener(changeListener)
		setSummaries(preferenceScreen)
	}

	override fun onPause() {
		super.onPause()
		preferenceScreen
			.sharedPreferences
			?.unregisterOnSharedPreferenceChangeListener(changeListener)
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

	private fun Activity.askToExportToFile() {
		// Write permission is only required before Android Q.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
			!requestWritePermission() { askToExportToFile() }
		) {
			return
		}
		askForName(R.string.save_as, Database.FILE_NAME) { name ->
			scope.launch {
				val messageId = if (exportDatabase(name)) {
					R.string.export_successful
				} else {
					R.string.export_failed
				}
				withContext(Dispatchers.Main) {
					Toast.makeText(
						this@askToExportToFile,
						messageId,
						Toast.LENGTH_LONG
					).show()
				}
			}
		}
	}

	companion object {
		const val PICK_FILE_RESULT_CODE = 1
	}
}

private fun Activity.restartApp() {
	startActivity(
		Intent(this, MainActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			putExtra(MainActivity.OPEN_PREFERENCES, true)
		}
	)
	finish()
	Runtime.getRuntime().exit(0)
}

fun Context.askForName(
	titleId: Int,
	preset: String,
	callback: (String) -> Any
) {
	// Dialogs don't have a parent layout.
	@SuppressLint("InflateParams")
	val view = LayoutInflater.from(this).inflate(
		R.layout.dialog_save_as, null
	)
	val nameView = view.findViewById<EditText>(R.id.name)
	nameView.setText(preset)
	AlertDialog.Builder(this)
		.setTitle(titleId)
		.setView(view)
		.setPositiveButton(android.R.string.ok) { _, _ ->
			callback(nameView.text.toString())
		}
		.setNegativeButton(android.R.string.cancel) { _, _ -> }
		.show()
}