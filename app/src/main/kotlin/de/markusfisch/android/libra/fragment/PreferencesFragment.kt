package de.markusfisch.android.libra.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
		designSpinner.init(R.array.design_names)
		designSpinner.setValue(R.array.design_values, prefs.design.toString())

		return view
	}

	override fun onStop() {
		super.onStop()
		designSpinner.getValue(R.array.design_values)?.let {
			val mode = it.toInt()
			if (mode != prefs.design) {
				prefs.design = mode
				// from AppCompat 1.1.0 invoking setDefaultNightMode()
				// will automatically update the app but since I want
				// to keep the minSdk, I need to do it manually
				restartApp(activity)
			}
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

private fun Spinner.getValue(valuesId: Int): String? {
	val values: Array<String> = resources.getStringArray(valuesId)
	return values[selectedItemPosition]
}

private fun Spinner.setValue(valuesId: Int, value: String) {
	val values: Array<String> = resources.getStringArray(valuesId)
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
		activity.startActivity(intent)
		activity.finish()
	}
	Runtime.getRuntime().exit(0)
}
