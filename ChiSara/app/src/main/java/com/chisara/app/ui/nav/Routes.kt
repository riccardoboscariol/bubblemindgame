package com.chisara.app.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val GUESS = "guess/{eventId}"
    const val REVEAL = "reveal"
    const val STATS = "stats"

    fun guess(eventId: Long) = "guess/$eventId"
}
