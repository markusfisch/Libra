package de.markusfisch.android.libra.fragment

import de.markusfisch.android.libra.adapter.IssuesAdapter
import de.markusfisch.android.libra.app.LibraApp
import de.markusfisch.android.libra.app.replaceFragment
import de.markusfisch.android.libra.database.DataSource
import de.markusfisch.android.libra.R

import android.app.AlertDialog
import android.database.Cursor
import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView

class IssuesFragment(): Fragment() {
	private lateinit var adapter: IssuesAdapter

	override fun onCreateView(
			inflater: LayoutInflater,
			container: ViewGroup?,
			state: Bundle?): View {
		val activity = getActivity()
		activity.setTitle(R.string.issues)
		adapter = IssuesAdapter(activity,
				LibraApp.data.getIssues())

		val view = inflater.inflate(
				R.layout.fragment_issues,
				container,
				false)

		val listView = view.findViewById(R.id.issues) as ListView
		listView.setEmptyView(view.findViewById(R.id.no_issues))
		listView.setAdapter(adapter)
		listView.setOnItemClickListener { parent, view, position, id ->
			showArguments(id)
		}
		listView.setOnItemLongClickListener { parent, view, position, id ->
			askForIssueName(id, adapter.getItem(position) as Cursor?)
			true
		}

		var addButton = view.findViewById(R.id.add)
		addButton.setOnClickListener { v ->
			showArguments(LibraApp.data.insertIssue())
		}

		return view
	}

	private fun showArguments(id: Long) {
		replaceFragment(getFragmentManager(),
				ArgumentsFragment.newInstance(id))
	}

	private fun askForIssueName(issueId: Long, cursor: Cursor?) {
		val context = getActivity()
		val view = LayoutInflater.from(context).inflate(
				R.layout.dialog_enter_name, null)
		val nameView = view.findViewById(R.id.name) as EditText
		nameView.setText(cursor?.getString(cursor.getColumnIndex(
				DataSource.ISSUES_NAME)))
		AlertDialog.Builder(context)
				.setView(view)
				.setPositiveButton(android.R.string.ok, { dialog, id ->
					updateIssueName(issueId,
							nameView.getText().toString())
				})
				.setNegativeButton(android.R.string.cancel, { dialog, id -> })
				.show()
	}

	private fun updateIssueName(id: Long, name: String) {
		LibraApp.data.updateIssueName(id, name)
		adapter.changeCursor(LibraApp.data.getIssues())
	}
}
