package de.markusfisch.android.libra.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.markusfisch.android.libra.R
import de.markusfisch.android.libra.app.addFragment
import de.markusfisch.android.libra.app.setFragment
import de.markusfisch.android.libra.fragment.IssuesFragment
import de.markusfisch.android.libra.fragment.PreferencesFragment
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : AppCompatActivity() {
	override fun onSupportNavigateUp(): Boolean {
		val fm = supportFragmentManager
		if (fm.backStackEntryCount > 0) {
			fm.popBackStack()
		} else {
			finish()
		}
		return true
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_main)
		initToolbar()

		if (state == null) {
			setFragment(supportFragmentManager, IssuesFragment())
			if (intent?.getBooleanExtra(OPEN_PREFERENCES, false) == true) {
				addFragment(supportFragmentManager, PreferencesFragment())
			}
		}
	}

	private fun initToolbar() {
		setSupportActionBar(toolbar)
		supportFragmentManager.addOnBackStackChangedListener {
			updateUpArrow()
		}
		updateUpArrow()
	}

	private fun updateUpArrow() {
		supportActionBar?.setDisplayHomeAsUpEnabled(
			supportFragmentManager.backStackEntryCount > 0
		)
	}

	companion object {
		const val OPEN_PREFERENCES = "open_preferences"
	}
}
