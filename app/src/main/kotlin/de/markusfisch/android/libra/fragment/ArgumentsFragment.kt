package de.markusfisch.android.libra.fragment

import de.markusfisch.android.libra.adapter.ArgumentsAdapter
import de.markusfisch.android.libra.app.LibraApp
import de.markusfisch.android.libra.app.Recommendation
import de.markusfisch.android.libra.database.DataSource
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
	private lateinit var listView: ListView
	private lateinit var editText: EditText
	private lateinit var cancelButton: View
	private lateinit var removeButton: View
	private lateinit var scaleView: ScaleView
	private var argumentId: Long = 0
	private var decisionId: Long = 0

	fun reloadList() {
		val cursor = LibraApp.data.getArguments(decisionId)
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
			decisionId = args.getLong(DECISION_ID, 0)
		}

		val activity = getActivity()
		var title = LibraApp.data.getDecisionName(decisionId)
		if (title.isEmpty()) {
			activity.setTitle(R.string.arguments)
		} else {
			activity.setTitle(title)
		}

		val cursor = LibraApp.data.getArguments(decisionId)
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

		listView = view.findViewById(R.id.arguments) as ListView
		listView.addHeaderView(scaleView, null, false)
		listView.setEmptyView(view.findViewById(R.id.no_arguments))
		listView.setAdapter(adapter)
		listView.setOnItemLongClickListener { parent, view, position, id ->
			editArgument(id)
			true
		}

		updateScale(cursor)

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
				scaleView.setText(R.string.weigh_arguments)
				return
			}
		} while (cursor.moveToNext())

		scaleView.setWeights(negative, positive)
		when (Recommendation.getRecommendation(negative, positive)) {
			Recommendation.YES ->
					scaleView.setText(R.string.do_it)
			Recommendation.MAYBE ->
					scaleView.setText(R.string.think_it_over)
			else ->
					scaleView.setText(R.string.do_not_do_it)
		}

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
			id = LibraApp.data.insertArgument(decisionId, text, 0)
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
		LibraApp.data.removeDecision(decisionId)
		getFragmentManager().popBackStack()
	}

	private fun sortArguments() {
		LibraApp.data.sortArguments(decisionId)
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
