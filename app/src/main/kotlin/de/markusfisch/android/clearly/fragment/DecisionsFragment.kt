package de.markusfisch.android.clearly.fragment

import de.markusfisch.android.clearly.adapter.DecisionsAdapter
import de.markusfisch.android.clearly.app.ClearlyApp
import de.markusfisch.android.clearly.app.replaceFragment
import de.markusfisch.android.clearly.R

import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView

class DecisionsFragment(): Fragment() {
	private lateinit var adapter: DecisionsAdapter
	private lateinit var listView: ListView

	override fun onCreateView(
			inflater: LayoutInflater,
			container: ViewGroup?,
			state: Bundle?): View {
		val activity = getActivity()
		activity.setTitle(R.string.decisions)
		adapter = DecisionsAdapter(activity,
				ClearlyApp.data.getDecisions())

		val view = inflater.inflate(
				R.layout.fragment_decisions,
				container,
				false)

		listView = view.findViewById(R.id.decisions) as ListView
		listView.setEmptyView(view.findViewById(R.id.no_decisions))
		listView.setAdapter(adapter)
		listView.setOnItemClickListener { parent, view, position, id ->
			replaceFragment(getFragmentManager(),
					ArgumentsFragment.newInstance(id))
		}

		return view
	}
}
