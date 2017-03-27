package de.markusfisch.android.clearly.app

enum class Recommendation {
	INCOMPLETE,
	YES,
	MAYBE,
	NO;

	companion object {
		fun getRecommendation(negative: Int, positive: Int): Recommendation {
			val ng = Math.abs(negative)
			if (positive == 0 && negative == 0) {
				return INCOMPLETE
			} else if (positive >= ng * 2) {
				return YES
			} else if (positive > ng) {
				return MAYBE
			} else {
				return NO
			}
		}
	}
}
