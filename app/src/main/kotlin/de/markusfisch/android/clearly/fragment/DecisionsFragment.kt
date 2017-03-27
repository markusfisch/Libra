package de.markusfisch.android.clearly.fragment

import de.markusfisch.android.clearly.adapter.DecisionsAdapter
import de.markusfisch.android.clearly.app.ClearlyApp
import de.markusfisch.android.clearly.app.replaceFragment
import de.markusfisch.android.clearly.database.DataSource
import de.markusfisch.android.clearly.R

import android.app.AlertDialog
import android.database.Cursor
import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView

class DecisionsFragment(): Fragment() {
	private lateinit var adapter: DecisionsAdapter

	override fun onCreateView(
			inflater: LayoutInflater,
			container: ViewGroup?,
			state: Bundle?): View {
		val activity = getActivity()
		activity.setTitle(R.string.app_name)
		adapter = DecisionsAdapter(activity,
				ClearlyApp.data.getDecisions())

		val view = inflater.inflate(
				R.layout.fragment_decisions,
				container,
				false)

		val listView = view.findViewById(R.id.decisions) as ListView
		listView.setEmptyView(view.findViewById(R.id.no_decisions))
		listView.setAdapter(adapter)
		listView.setOnItemClickListener { parent, view, position, id ->
			showArguments(id)
		}
		listView.setOnItemLongClickListener { parent, view, position, id ->
			askForDecisionName(id, adapter.getItem(position) as Cursor?)
			true
		}

		var addButton = view.findViewById(R.id.add)
		addButton.setOnClickListener { v ->
			showArguments(ClearlyApp.data.insertDecision())
		}

		return view
	}

	private fun showArguments(id: Long) {
		replaceFragment(getFragmentManager(),
				ArgumentsFragment.newInstance(id))
	}

	private fun askForDecisionName(decisionId: Long, cursor: Cursor?) {
		val context = getActivity()
		val view = LayoutInflater.from(context).inflate(
				R.layout.dialog_enter_name, null)
		val nameView = view.findViewById(R.id.name) as EditText
		nameView.setText(cursor?.getString(cursor?.getColumnIndex(
				DataSource.DECISIONS_NAME)))
		AlertDialog.Builder(context)
				.setView(view)
				.setPositiveButton(android.R.string.ok, { dialog, id ->
					updateDecisionName(decisionId,
							nameView.getText().toString())
				})
				.setNegativeButton(android.R.string.cancel, { dialog, id -> })
				.show()
	}

	private fun updateDecisionName(id: Long, name: String) {
		ClearlyApp.data.updateDecisionName(id, name)
		adapter.changeCursor(ClearlyApp.data.getDecisions())
	}
}
