package de.markusfisch.android.libra.fragment

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ListView
import android.widget.SeekBar
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.adapter.ArgumentsAdapter
import de.markusfisch.android.libra.app.db
import de.markusfisch.android.libra.app.shareText
import de.markusfisch.android.libra.database.Database
import de.markusfisch.android.libra.widget.ScaleView

class ArgumentsFragment : Fragment() {
	private val actionModeCallback = object : ActionMode.Callback {
		override fun onCreateActionMode(
			mode: ActionMode,
			menu: Menu
		): Boolean {
			mode.menuInflater.inflate(
				R.menu.fragment_argument_edit,
				menu
			)
			return true
		}

		override fun onPrepareActionMode(
			mode: ActionMode,
			menu: Menu
		): Boolean {
			return false
		}

		override fun onActionItemClicked(
			mode: ActionMode,
			item: MenuItem
		): Boolean {
			return when (item.itemId) {
				R.id.remove_argument -> {
					askToRemoveArgument(activity, adapter.selectedId)
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
	private lateinit var listView: ListView
	private lateinit var weightBar: SeekBar
	private lateinit var argumentInput: EditText
	private lateinit var scaleView: ScaleView
	private var actionMode: ActionMode? = null
	private var issueId: Long = 0

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		if (arguments != null) {
			issueId = arguments.getLong(ISSUE_ID, 0)
		}

		val title = db.getIssueName(issueId)
		if (title.isEmpty()) {
			activity.setTitle(R.string.arguments)
		} else {
			activity.title = title
		}

		val cursor = db.getArguments(issueId) ?: return null
		adapter = ArgumentsAdapter(activity, cursor)

		val view = inflater.inflate(
			R.layout.fragment_arguments,
			container,
			false
		)

		weightBar = view.findViewById(R.id.weight)

		argumentInput = view.findViewById(R.id.argument)
		argumentInput.setOnEditorActionListener { _, actionId, _ ->
			when (actionId) {
				EditorInfo.IME_ACTION_GO,
				EditorInfo.IME_ACTION_SEND,
				EditorInfo.IME_ACTION_DONE,
				EditorInfo.IME_ACTION_NEXT,
				EditorInfo.IME_NULL -> saveArgument()
				else -> false
			}
		}

		val enterButton = view.findViewById<View>(R.id.save_argument)
		enterButton.setOnClickListener { saveArgument() }

		scaleView = ScaleView(activity)
		updateScale(cursor)

		listView = view.findViewById(R.id.arguments)
		listView.addHeaderView(scaleView, null, false)
		listView.emptyView = view.findViewById(R.id.no_arguments)
		listView.adapter = adapter
		listView.setOnItemClickListener { _, v, _, id ->
			v.isSelected = true
			editArgument(id)
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
		outState.putLong(ARGUMENTS_ID, adapter.selectedId)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_arguments, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.share_arguments -> {
				shareArguments()
				true
			}
			R.id.edit_issue -> {
				askForIssueName(
					context,
					issueId,
					db.getIssueName(issueId)
				) { title ->
					activity?.title = title
				}
				true
			}
			R.id.sort_arguments -> {
				sortArguments()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun reloadList() {
		val cursor = db.getArguments(issueId) ?: return
		adapter.changeCursor(cursor)
		updateScale(cursor)
	}

	private fun updateScale(cursor: Cursor) {
		if (!cursor.moveToFirst()) {
			return
		}

		val weightIndex = cursor.getColumnIndex(Database.ARGUMENTS_WEIGHT)
		var positive = 0
		var negative = 0

		do {
			val weight = cursor.getInt(weightIndex)
			when {
				weight > 0 -> positive += weight
				weight < 0 -> negative += -weight
				else -> {
					scaleView.setWeights(-1, -1)
					cursor.moveToFirst()
					return
				}
			}
		} while (cursor.moveToNext())

		scaleView.setWeights(negative, positive)
		cursor.moveToFirst()
	}

	private fun saveArgument(): Boolean {
		val text = argumentInput.text.toString().trim()
		if (text.isEmpty()) {
			return false
		}
		val weight = weightBar.progress - WEIGHT_BAR_SHIFT
		if (adapter.selectedId > 0) {
			db.updateArgument(adapter.selectedId, text, weight)
			val state = listView.onSaveInstanceState()
			reloadList()
			listView.onRestoreInstanceState(state)
		} else {
			db.insertArgument(issueId, text, weight)
			reloadList()
			listView.smoothScrollToPosition(adapter.count)
		}
		closeActionMode()
		return true
	}

	private fun askToRemoveArgument(context: Context, argId: Long) {
		AlertDialog.Builder(context)
			.setMessage(R.string.really_remove_argument)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				removeArgument(argId)
				closeActionMode()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun removeArgument(id: Long) {
		db.removeArgument(id)
		reloadList()
	}

	private fun shareArguments() {
		val ctx = context ?: return
		db.getArguments(issueId, true)?.use { cursor ->
			if (!cursor.moveToFirst()) {
				return@shareArguments
			}
			val textIndex = cursor.getColumnIndex(Database.ARGUMENTS_TEXT)
			val weightIndex = cursor.getColumnIndex(Database.ARGUMENTS_WEIGHT)
			val sb = StringBuilder()
			var headerWritten = false
			do {
				val weight = cursor.getInt(weightIndex)
				if (weight > -1) {
					if (headerWritten) {
						sb.append("\n")
					}
					headerWritten = false
				}
				if (!headerWritten) {
					sb.append(if (weight < 0) {
						getString(R.string.header_cons)
					} else {
						getString(R.string.header_pros)
					})
					sb.append("\n")
					headerWritten = true
				}
				sb.append("* %3d ".format(weight))
				sb.append(cursor.getString(textIndex))
				sb.append("\n")
			} while (cursor.moveToNext())
			shareText(ctx, sb.toString())
		}
	}

	private fun editArgument(id: Long) {
		adapter.selectedId = id
		db.getArgument(id)?.let {
			weightBar.progress = it.weight + WEIGHT_BAR_SHIFT
			argumentInput.setText(it.text)
		} ?: return
		val a = activity
		if (actionMode == null && a is AppCompatActivity) {
			actionMode = a.delegate.startSupportActionMode(
				actionModeCallback
			)
		}
	}

	private fun sortArguments() {
		db.sortArguments(issueId)
		reloadList()
	}

	private fun closeActionMode() {
		actionMode?.finish()
		actionMode = null
		argumentInput.setText("")
		weightBar.progress = WEIGHT_BAR_SHIFT
		adapter.selectedId = 0L
		adapter.notifyDataSetChanged()
	}

	companion object {
		private const val ISSUE_ID = "issue_id"
		private const val ARGUMENTS_ID = "argumentId"
		private const val WEIGHT_BAR_SHIFT = 10

		fun newInstance(issueId: Long): ArgumentsFragment {
			val args = Bundle()
			args.putLong(ISSUE_ID, issueId)

			val fragment = ArgumentsFragment()
			fragment.arguments = args
			return fragment
		}
	}
}
