package de.markusfisch.android.libra.app

import kotlin.math.abs

enum class Recommendation {
	INCOMPLETE,
	YES,
	MAYBE,
	NO;

	companion object {
		fun getRecommendation(negative: Int, positive: Int): Recommendation {
			val ng = abs(negative)
			return if (positive == 0 && negative == 0) {
				INCOMPLETE
			} else if (positive >= ng * 2) {
				YES
			} else if (positive >= ng) {
				MAYBE
			} else {
				NO
			}
		}
	}
}
