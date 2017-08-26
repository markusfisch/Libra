package de.markusfisch.android.libra.fragment

import de.markusfisch.android.libra.adapter.ArgumentsAdapter
import de.markusfisch.android.libra.app.LibraApp
import de.markusfisch.android.libra.database.DataSource
import de.markusfisch.android.libra.widget.ArgumentListView
import de.markusfisch.android.libra.widget.ScaleView
import de.markusfisch.android.libra.R

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText

class ArgumentsFragment : Fragment() {
	companion object {
		private val ISSUE_ID = "issue_id"
		private val ARGUMENTS_ID = "argumentId"

		fun newInstance(issueId: Long): ArgumentsFragment {
			val args = Bundle()
			args.putLong(ISSUE_ID, issueId)

			val fragment = ArgumentsFragment()
			fragment.arguments = args
			return fragment
		}
	}

	private val actionModeCallback = object : ActionMode.Callback {
		override fun onCreateActionMode(
				mode: ActionMode,
				menu: Menu): Boolean {
			mode.menuInflater.inflate(
					R.menu.fragment_argument_edit,
					menu)
			return true
		}

		override fun onPrepareActionMode(
				mode: ActionMode,
				menu: Menu): Boolean {
			return false
		}

		override fun onActionItemClicked(
				mode: ActionMode,
				item: MenuItem): Boolean {
			return when (item.itemId) {
				R.id.remove_argument -> {
					askToRemoveArgument(activity, argumentId)
					closeActionMode()
					true
				}
				else -> false
			}
		}

		override fun onDestroyActionMode(mode: ActionMode) {
			closeActionMode()
		}
	}

	private lateinit var adapter: ArgumentsAdapter
	private lateinit var listView: ArgumentListView
	private lateinit var editText: EditText
	private lateinit var scaleView: ScaleView
	private var actionMode: ActionMode? = null
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
		if (arguments != null) {
			issueId = arguments.getLong(ISSUE_ID, 0)
		}

		val title = LibraApp.data.getIssueName(issueId)
		if (title.isEmpty()) {
			activity.setTitle(R.string.arguments)
		} else {
			activity.title = title
		}

		val cursor = LibraApp.data.getArguments(issueId)
		adapter = ArgumentsAdapter(activity, cursor)

		val view = inflater.inflate(
				R.layout.fragment_arguments,
				container,
				false)

		editText = view.findViewById(R.id.argument)
		editText.setOnEditorActionListener { _, actionId, _ ->
			when (actionId) {
				EditorInfo.IME_ACTION_GO,
				EditorInfo.IME_ACTION_SEND,
				EditorInfo.IME_ACTION_DONE,
				EditorInfo.IME_ACTION_NEXT,
				EditorInfo.IME_NULL -> saveArgument()
				else -> false
			}
		}

		val enterButton: View = view.findViewById(R.id.enter_argument)
		enterButton.setOnClickListener { _ -> saveArgument() }

		scaleView = ScaleView(activity)
		updateScale(cursor)

		listView = view.findViewById(R.id.arguments)
		listView.addHeaderView(scaleView, null, false)
		listView.emptyView = view.findViewById(R.id.no_arguments)
		listView.adapter = adapter
		listView.setOnItemLongClickListener { _, v, _, id ->
			if (!listView.isWeighing) {
				v.isSelected = true
				editArgument(id)
				true
			} else {
				false
			}
		}

		if (state != null) {
			val id = state.getLong(ARGUMENTS_ID, 0)
			if (id > 0) {
				editArgument(id)
			}
		}

		return view
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putLong(ARGUMENTS_ID, argumentId)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_arguments, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.sort_arguments -> {
				sortArguments()
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

	private fun saveArgument(): Boolean {
		val text = editText.text.toString()
		if (text.trim().isEmpty()) {
			return false
		}
		val id: Long
		if (argumentId > 0) {
			LibraApp.data.updateArgumentText(argumentId, text)
			id = argumentId
		} else {
			id = LibraApp.data.insertArgument(issueId, text, 0)
		}
		closeActionMode()
		reloadList()
		listView.setSelection(getItemPosition(id))
		return true
	}

	private fun askToRemoveArgument(context: Context, argId: Long) {
		AlertDialog.Builder(context)
				.setMessage(R.string.really_remove_argument)
				.setPositiveButton(android.R.string.ok, { _, _ ->
					removeArgument(argId)
					closeActionMode()
				})
				.setNegativeButton(android.R.string.cancel, { _, _ ->
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
		val a = activity
		if (actionMode == null && a is AppCompatActivity) {
			actionMode = a.getDelegate().startSupportActionMode(
					actionModeCallback)
		}
	}

	private fun sortArguments() {
		LibraApp.data.sortArguments(issueId)
		reloadList()
	}

	private fun closeActionMode() {
		actionMode?.finish()
		actionMode = null
		editText.setText("")
		argumentId = 0
		adapter.notifyDataSetChanged()
	}

	private fun getItemPosition(id: Long): Int {
		var i = adapter.count
		while (i-- > 0) {
			if (adapter.getItemId(i) == id) {
				return i
			}
		}
		return -1
	}
}
