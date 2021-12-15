package de.markusfisch.android.libra.app

import android.annotation.SuppressLint
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import de.markusfisch.android.libra.R

fun FragmentManager.setFragment(fragment: Fragment) {
	getTransaction(fragment).commit()
}

fun FragmentManager.addFragment(fragment: Fragment) {
	getTransaction(fragment).addToBackStack(null).commit()
}

@SuppressLint("CommitTransaction")
private fun FragmentManager.getTransaction(
	fragment: Fragment
): FragmentTransaction = beginTransaction().replace(
	R.id.content_frame,
	fragment
)
