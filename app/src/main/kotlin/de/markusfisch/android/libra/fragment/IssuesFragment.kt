package de.markusfisch.android.libra.fragment

import android.annotation.SuppressLint
import de.markusfisch.android.libra.adapter.IssuesAdapter
import de.markusfisch.android.libra.app.db
import de.markusfisch.android.libra.app.addFragment
import de.markusfisch.android.libra.database.Database
import de.markusfisch.android.libra.R

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView

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
				R.id.edit_issue -> {
					askForIssueName(issue.id, getItemText(issue.position))
					closeActionMode()
					true
				}
				R.id.remove_issue -> {
					askToRemoveIssue(activity, issue.id)
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

	private val issue = Issue(0L, 0)

	private lateinit var adapter: IssuesAdapter
	private var actionMode: ActionMode? = null

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View {
		activity.setTitle(R.string.issues)
		adapter = IssuesAdapter(activity, db.getIssues())

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
			issue.id = id
			issue.position = position
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
		adapter.notifyDataSetChanged()
	}

	private fun askToRemoveIssue(context: Context, issueId: Long) {
		AlertDialog.Builder(context)
			.setMessage(R.string.really_remove_issue)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				removeIssue(issueId)
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
	}

	private fun removeIssue(issueId: Long) {
		db.removeIssue(issueId)
		updateList()
	}

	private fun getItemText(position: Int): String? {
		val cursor = adapter.getItem(position) as Cursor?
		return cursor?.getString(
			cursor.getColumnIndex(
				Database.ISSUES_NAME
			)
		)
	}

	// dialogs don't have a parent layout
	@SuppressLint("InflateParams")
	private fun askForIssueName(issueId: Long, text: String?) {
		val context = activity
		val view = LayoutInflater.from(context).inflate(
			R.layout.dialog_enter_name, null
		)
		val nameView = view.findViewById<EditText>(R.id.name)
		nameView.setText(text)
		AlertDialog.Builder(context)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				updateIssueName(
					issueId,
					nameView.text.toString()
				)
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
	}

	private fun updateIssueName(id: Long, name: String) {
		db.updateIssueName(id, name)
		updateList()
	}

	private fun updateList() {
		adapter.changeCursor(db.getIssues())
	}

	private data class Issue(var id: Long, var position: Int)
}
