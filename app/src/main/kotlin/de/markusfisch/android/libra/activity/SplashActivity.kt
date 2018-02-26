package de.markusfisch.android.libra.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class SplashActivity() : AppCompatActivity() {
	override fun onCreate(state: Bundle?) {
		super.onCreate(state)

		// it's important _not_ to inflate a layout file here
		// because that would happen after the app is fully
		// initialized

		startActivity(Intent(this, MainActivity::class.java))
		finish()
	}
}
