package de.markusfisch.android.clearly.app

import de.markusfisch.android.clearly.R

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction

fun setFragment(fm: FragmentManager, fragment: Fragment) {
	getTransaction(fm, fragment).commit()
}

fun replaceFragment(fm: FragmentManager, fragment: Fragment) {
	getTransaction(fm, fragment).addToBackStack(null).commit()
}

fun getTransaction(
		fm: FragmentManager,
		fragment: Fragment): FragmentTransaction {
	return fm.beginTransaction().replace(R.id.content_frame, fragment)
}
