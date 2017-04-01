package de.markusfisch.android.libra.fragment

import de.markusfisch.android.libra.adapter.ArgumentsAdapter
import de.markusfisch.android.libra.app.LibraApp
import de.markusfisch.android.libra.database.DataSource
import de.markusfisch.android.libra.widget.ArgumentListView
import de.markusfisch.android.libra.widget.ScaleView
import de.markusfisch.android.libra.R

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText

class ArgumentsFragment(): Fragment() {
	companion object {
		private val ISSUE_ID = "issue_id"

		fun newInstance(issueId: Long): ArgumentsFragment {
			val args = Bundle()
			args.putLong(ISSUE_ID, issueId)

			val fragment = ArgumentsFragment()
			fragment.setArguments(args)
			return fragment
		}
	}

	private lateinit var adapter: ArgumentsAdapter
	private lateinit var listView: ArgumentListView
	private lateinit var editText: EditText
	private lateinit var cancelButton: View
	private lateinit var removeButton: View
	private lateinit var scaleView: ScaleView
	private var argumentId: Long = 0
	private var issueId: Long = 0

	fun reloadList() {
		val cursor = LibraApp.data.getArguments(issueId)
		adapter.changeCursor(cursor)
		updateScale(cursor)
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
			inflater: LayoutInflater,
			container: ViewGroup?,
			state: Bundle?): View {
		var args = getArguments()
		if (args != null) {
			issueId = args.getLong(ISSUE_ID, 0)
		}

		val activity = getActivity()
		var title = LibraApp.data.getIssueName(issueId)
		if (title.isEmpty()) {
			activity.setTitle(R.string.arguments)
		} else {
			activity.setTitle(title)
		}

		val cursor = LibraApp.data.getArguments(issueId)
		adapter = ArgumentsAdapter(activity, cursor)

		val view = inflater.inflate(
				R.layout.fragment_arguments,
				container,
				false)

		editText = view.findViewById(R.id.enter_argument) as EditText
		editText.setOnEditorActionListener { v, actionId, event ->
			when (actionId) {
				EditorInfo.IME_ACTION_GO,
				EditorInfo.IME_ACTION_SEND,
				EditorInfo.IME_ACTION_DONE,
				EditorInfo.IME_ACTION_NEXT,
				EditorInfo.IME_NULL -> saveArgument(v.getText().toString())
				else -> false
			}
		}

		cancelButton = view.findViewById(R.id.cancel_editing)
		cancelButton.setOnClickListener { v -> resetInput() }

		removeButton = view.findViewById(R.id.remove_argument)
		removeButton.setOnClickListener { v ->
			if (argumentId > 0) {
				askToRemoveArgument(activity, argumentId)
				resetInput()
			}
		}

		scaleView = ScaleView(activity)
		updateScale(cursor)

		listView = view.findViewById(R.id.arguments) as ArgumentListView
		listView.addHeaderView(scaleView, null, false)
		listView.setEmptyView(view.findViewById(R.id.no_arguments))
		listView.setAdapter(adapter)
		listView.setOnItemLongClickListener { parent, view, position, id ->
			if (!listView.isWeighing) {
				editArgument(id)
				true
			} else {
				false
			}
		}

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_arguments, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.getItemId()) {
			R.id.sort_arguments -> {
				sortArguments()
				true
			}
			R.id.remove_issue-> {
				askToRemoveIssue(getActivity())
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun updateScale(cursor: Cursor) {
		if (!cursor.moveToFirst()) {
			return
		}

		val weightIndex = cursor.getColumnIndex(DataSource.ARGUMENTS_WEIGHT)
		var positive = 0
		var negative = 0

		do {
			val weight = cursor.getInt(weightIndex)
			if (weight > 0) {
				positive += weight
			} else if (weight < 0) {
				negative += -weight
			} else {
				scaleView.setWeights(-1, -1)
				return
			}
		} while (cursor.moveToNext())

		scaleView.setWeights(negative, positive)
		cursor.moveToFirst()
	}

	private fun saveArgument(text: String): Boolean {
		if (text.trim().length < 1) {
			return false
		}
		var id: Long
		if (argumentId > 0) {
			LibraApp.data.updateArgumentText(argumentId, text)
			id = argumentId
		} else {
			id = LibraApp.data.insertArgument(issueId, text, 0)
		}
		resetInput()
		reloadList()
		listView.setSelection(getItemPosition(id))
		return true
	}

	private fun askToRemoveArgument(context: Context, argId: Long) {
		AlertDialog.Builder(context)
				.setMessage(R.string.really_remove_argument)
				.setPositiveButton(android.R.string.ok, { dialog, id ->
					removeArgument(argId)
				})
				.setNegativeButton(android.R.string.cancel, { dialog, id ->
				})
				.show()
	}

	private fun removeArgument(id: Long) {
		LibraApp.data.removeArgument(id)
		reloadList()
	}

	private fun editArgument(id: Long) {
		argumentId = id
		editText.setText(LibraApp.data.getArgumentText(id))
		cancelButton.setVisibility(View.VISIBLE)
		removeButton.setVisibility(View.VISIBLE)
	}

	private fun askToRemoveIssue(context: Context) {
		AlertDialog.Builder(context)
				.setMessage(R.string.really_remove_issue)
				.setPositiveButton(android.R.string.ok, { dialog, id ->
					removeIssue()
				})
				.setNegativeButton(android.R.string.cancel, { dialog, id -> })
				.show()
	}

	private fun removeIssue() {
		LibraApp.data.removeIssue(issueId)
		getFragmentManager().popBackStack()
	}

	private fun sortArguments() {
		LibraApp.data.sortArguments(issueId)
		reloadList()
	}

	private fun resetInput() {
		editText.setText("")
		argumentId = 0
		removeButton.setVisibility(View.GONE)
		cancelButton.setVisibility(View.GONE)
	}

	private fun getItemPosition(id: Long): Int {
		var i = adapter.getCount()
		while (i-- > 0) {
			if (adapter.getItemId(i) == id) {
				return i
			}
		}
		return -1
	}
}
