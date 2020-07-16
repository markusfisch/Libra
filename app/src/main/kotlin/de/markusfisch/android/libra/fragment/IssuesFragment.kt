package de.markusfisch.android.libra.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.*
import android.widget.EditText
import android.widget.ListView
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.adapter.IssuesAdapter
import de.markusfisch.android.libra.app.addFragment
import de.markusfisch.android.libra.app.db
import de.markusfisch.android.libra.database.Database

class IssuesFragment : Fragment() {
	private val actionModeCallback = object : ActionMode.Callback {
		override fun onCreateActionMode(
			mode: ActionMode,
			menu: Menu
		): Boolean {
			mode.menuInflater.inflate(
				R.menu.fragment_issue_edit,
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
				R.id.duplicate_issue -> {
					db.duplicateIssue(adapter.selectedId)
					updateList()
					closeActionMode()
					true
				}
				R.id.edit_issue -> {
					askForIssueName(
						context,
						adapter.selectedId,
						getItemText(adapter.selectedPosition)
					) {
						updateList()
					}
					closeActionMode()
					true
				}
				R.id.remove_issue -> {
					askToRemoveIssue(activity, adapter.selectedId) {
						updateList()
					}
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

	private lateinit var adapter: IssuesAdapter
	private var actionMode: ActionMode? = null

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		activity.setTitle(R.string.issues)

		val cursor = db.getIssues() ?: return null
		adapter = IssuesAdapter(activity, cursor)

		val view = inflater.inflate(
			R.layout.fragment_issues,
			container,
			false
		)

		val listView = view.findViewById<ListView>(R.id.issues)
		listView.emptyView = view.findViewById(R.id.no_issues)
		listView.adapter = adapter
		listView.setOnItemClickListener { _, _, _, id ->
			showArguments(id)
		}
		listView.setOnItemLongClickListener { _, v, position, id ->
			v.isSelected = true
			adapter.select(id, position)
			val a = activity
			if (actionMode == null && a is AppCompatActivity) {
				actionMode = a.delegate.startSupportActionMode(
					actionModeCallback
				)
			}
			true
		}

		val addButton = view.findViewById<View>(R.id.add)
		addButton.setOnClickListener {
			showArguments(db.insertIssue())
		}

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_issues, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.preferences -> {
				addFragment(fragmentManager, PreferencesFragment())
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun showArguments(id: Long) {
		closeActionMode()
		addFragment(
			fragmentManager,
			ArgumentsFragment.newInstance(id)
		)
	}

	private fun closeActionMode() {
		actionMode?.finish()
		actionMode = null
		adapter.clearSelection()
		adapter.notifyDataSetChanged()
	}

	private fun getItemText(position: Int): String? {
		val cursor = adapter.getItem(position) as Cursor?
		return cursor?.getString(
			cursor.getColumnIndex(
				Database.ISSUES_NAME
			)
		)
	}

	private fun updateList() {
		adapter.changeCursor(db.getIssues())
	}
}

// dialogs don't have a parent layout
@SuppressLint("InflateParams")
fun askForIssueName(
	context: Context,
	issueId: Long,
	text: String?,
	update: (name: String) -> Unit
) {
	val view = LayoutInflater.from(context).inflate(
		R.layout.dialog_enter_name, null
	)
	val nameView = view.findViewById<EditText>(R.id.name)
	nameView.setText(text)
	AlertDialog.Builder(context)
		.setView(view)
		.setPositiveButton(android.R.string.ok) { _, _ ->
			val name = nameView.text.toString()
			db.updateIssueName(issueId, name)
			update(name)
		}
		.setNegativeButton(android.R.string.cancel) { _, _ -> }
		.show()
}

fun askToRemoveIssue(context: Context, issueId: Long, update: () -> Unit) {
	AlertDialog.Builder(context)
		.setMessage(R.string.really_remove_issue)
		.setPositiveButton(android.R.string.ok) { _, _ ->
			db.removeIssue(issueId)
			update()
		}
		.setNegativeButton(android.R.string.cancel) { _, _ -> }
		.show()
}
