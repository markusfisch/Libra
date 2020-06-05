package de.markusfisch.android.libra.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.activity.MainActivity
import de.markusfisch.android.libra.app.prefs

class PreferencesFragment : Fragment() {
	private lateinit var designSpinner: Spinner

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		activity?.setTitle(R.string.preferences)

		val view = inflater.inflate(
			R.layout.fragment_preferences,
			container,
			false
		)

		designSpinner = view.findViewById(R.id.design)
		designSpinner.apply {
			init(R.array.design_names)
			setValue(R.array.design_values, prefs.design.toString())
			onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
				val values = resources.getStringArray(R.array.design_values)

				override fun onNothingSelected(parent: AdapterView<*>?) {
				}

				override fun onItemSelected(
					parent: AdapterView<*>?,
					view: View?,
					position: Int,
					id: Long
				) {
					updateDesign(values[position].toInt())
				}
			}
		}

		return view
	}

	private fun updateDesign(mode: Int) {
		if (mode != prefs.design) {
			prefs.design = mode
			// setDefaultNightMode() in AppCompat 1.1.0 will automatically
			// update the app but since I want to keep the minSdk, I need
			// to restart the whole app to make sure the night mode setting
			// takes effect. Simply recreating the Activity doesn't work
			// when returning to MODE_NIGHT_FOLLOW_SYSTEM.
			restartApp(activity)
		}
	}
}

private fun Spinner.init(namesId: Int) {
	adapter = ArrayAdapter.createFromResource(
		context,
		namesId,
		android.R.layout.simple_spinner_item
	).apply {
		setDropDownViewResource(
			android.R.layout.simple_spinner_dropdown_item
		)
	}
}

private fun Spinner.setValue(valuesId: Int, value: String) {
	val values = resources.getStringArray(valuesId)
	for (i in values.indices) {
		if (values[i] == value) {
			setSelection(i)
			return
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
