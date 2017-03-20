package de.markusfisch.android.clearly.fragment

import de.markusfisch.android.clearly.adapter.ArgumentsAdapter
import de.markusfisch.android.clearly.app.ClearlyApp
import de.markusfisch.android.clearly.app.replaceFragment
import de.markusfisch.android.clearly.R

import android.app.AlertDialog
import android.content.Context
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
import android.widget.ListView

class ArgumentsFragment(): Fragment() {
	companion object {
		private val DECISION_ID = "decision_id"

		fun newInstance(decisionId: Long): ArgumentsFragment {
			val args = Bundle()
			args.putLong(DECISION_ID, decisionId)

			val fragment = ArgumentsFragment()
			fragment.setArguments(args)
			return fragment
		}
	}

	private lateinit var adapter: ArgumentsAdapter
	private lateinit var editText: EditText
	private lateinit var cancelButton: View
	private lateinit var removeButton: View
	private var argumentId: Long = 0
	private var decisionId: Long = 0

	fun reloadList() {
		adapter.changeCursor(ClearlyApp.data.getArguments(decisionId))
	}

	fun editArgument(id: Long) {
		argumentId = id
		editText.setText(ClearlyApp.data.getArgumentText(id))
		cancelButton.setVisibility(View.VISIBLE)
		removeButton.setVisibility(View.VISIBLE)
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
			decisionId = args.getLong(DECISION_ID, 0)
		}

		val activity = getActivity()
		var title = ClearlyApp.data.getDecisionName(decisionId)
		if (title.isEmpty()) {
			activity.setTitle(R.string.arguments)
		} else {
			activity.setTitle(title)
		}

		adapter = ArgumentsAdapter(activity,
				ClearlyApp.data.getArguments(decisionId))

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

		val listView = view.findViewById(R.id.arguments) as ListView
		listView.addHeaderView(inflater.inflate(
				R.layout.header_arguments,
				listView,
				false), null, false)
		listView.setEmptyView(view.findViewById(R.id.no_arguments))
		listView.setAdapter(adapter)

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_arguments, menu)

		val hasId = decisionId > 0
		menu.findItem(R.id.decisions_list).setVisible(!hasId)
		menu.findItem(R.id.new_decision).setVisible(!hasId)
		menu.findItem(R.id.remove_decision).setVisible(hasId)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.getItemId()) {
			R.id.decisions_list -> {
				replaceFragment(getFragmentManager(), DecisionsFragment())
				true
			}
			R.id.new_decision -> {
				newDecision()
				true
			}
			R.id.remove_decision-> {
				removeDecision()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun saveArgument(text: String): Boolean {
		if (text.trim().length < 1) {
			return false
		}
		if (argumentId > 0) {
			ClearlyApp.data.updateArgumentText(argumentId, text)
		} else {
			ClearlyApp.data.insertArgument(decisionId, text, 0)
		}
		resetInput()
		reloadList()
		return true
	}

	private fun askToRemoveArgument(context: Context, argId: Long) {
		AlertDialog.Builder(context)
				.setMessage(R.string.really_remove)
				.setPositiveButton(android.R.string.ok, { dialog, id ->
						removeArgument(argId)
				})
				.setNegativeButton(android.R.string.cancel, { dialog, id ->
				})
				.show()
	}

	private fun removeArgument(id: Long) {
		ClearlyApp.data.removeArgument(id)
		reloadList()
	}

	private fun newDecision(discard: Boolean = false) {
		if (discard) {
			ClearlyApp.data.removeDecision(0)
		} else if (decisionId == 0L) {
			ClearlyApp.data.fileArguments(0,
					ClearlyApp.data.insertDecision("test"))
		}
		decisionId = 0
		reloadList()
	}

	private fun removeDecision() {
		if (decisionId > 0) {
			ClearlyApp.data.removeDecision(decisionId)
			getFragmentManager().popBackStack()
		}
	}

	private fun resetInput() {
		editText.setText("")
		argumentId = 0
		removeButton.setVisibility(View.GONE)
		cancelButton.setVisibility(View.GONE)
	}
}
