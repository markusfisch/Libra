package de.markusfisch.android.clearly.fragment

import de.markusfisch.android.clearly.adapter.ArgumentsAdapter
import de.markusfisch.android.clearly.app.ClearlyApp
import de.markusfisch.android.clearly.app.replaceFragment
import de.markusfisch.android.clearly.database.DataSource
import de.markusfisch.android.clearly.widget.RecommendationView
import de.markusfisch.android.clearly.R

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
import android.widget.ListView
import android.widget.TextView

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
	private lateinit var recommendationView: RecommendationView
	private var argumentId: Long = 0
	private var decisionId: Long = 0

	fun reloadList() {
		val cursor = ClearlyApp.data.getArguments(decisionId)
		adapter.changeCursor(cursor)
		setRecommendation(cursor)
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

		val cursor = ClearlyApp.data.getArguments(decisionId)
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

		recommendationView = RecommendationView(activity)

		val listView = view.findViewById(R.id.arguments) as ListView
		listView.addHeaderView(recommendationView, null, false)
		listView.setEmptyView(view.findViewById(R.id.no_arguments))
		listView.setAdapter(adapter)

		setRecommendation(cursor)

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
			R.id.remove_decision-> {
				askToRemoveDecision(getActivity())
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun setRecommendation(cursor: Cursor) {
		if (!cursor.moveToFirst()) {
android.util.Log.e("mfdbg", "mfdbg: cannot move to first")
			return
		}

		val weightIndex = cursor.getColumnIndex(
				DataSource.ARGUMENTS_WEIGHT)
		var positive = 0
		var negative = 0

		do {
			val weight = cursor.getInt(weightIndex)
			if (weight > 0) {
				positive += weight
			} else if (weight < 0) {
				negative += -weight
			} else {
android.util.Log.d("mfdbg", "mfdbg: setWeight()")
				recommendationView.setWeight(0, 0)
				recommendationView.setText(R.string.weigh_arguments)
				return
			}
		} while (cursor.moveToNext())

android.util.Log.d("mfdbg", "mfdbg: setWeight($negative, $positive)")
		recommendationView.setWeight(negative, positive)
		cursor.moveToFirst()

		if (positive >= negative * 2) {
			recommendationView.setText(R.string.do_it)
		} else if (positive > negative) {
			recommendationView.setText(R.string.think_it_over)
		} else {
			recommendationView.setText(R.string.do_not_do_it)
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
				.setMessage(R.string.really_remove_argument)
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

	private fun askToRemoveDecision(context: Context) {
		AlertDialog.Builder(context)
				.setMessage(R.string.really_remove_decision)
				.setPositiveButton(android.R.string.ok, { dialog, id ->
						removeDecision()
				})
				.setNegativeButton(android.R.string.cancel, { dialog, id -> })
				.show()
	}

	private fun removeDecision() {
		ClearlyApp.data.removeDecision(decisionId)
		getFragmentManager().popBackStack()
	}

	private fun sortArguments() {
		ClearlyApp.data.sortArguments(decisionId)
		reloadList()
	}

	private fun resetInput() {
		editText.setText("")
		argumentId = 0
		removeButton.setVisibility(View.GONE)
		cancelButton.setVisibility(View.GONE)
	}
}
