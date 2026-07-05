package com.chisara.app.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun penaltyDisabled_yieldsZeroPenaltyConfig() {
        val config = AppSettings(penaltyEnabled = false, wrongAnswerPenalty = 20).toScoringConfig()
        assertEquals(0, config.wrongAnswerPenalty)
    }

    @Test
    fun penaltyEnabled_yieldsNegativePenaltyConfig() {
        val config = AppSettings(penaltyEnabled = true, wrongAnswerPenalty = 20).toScoringConfig()
        assertEquals(-20, config.wrongAnswerPenalty)
    }

    @Test
    fun defaults_trackAllThreeApps_andNoGroups() {
        val settings = AppSettings()
        assertEquals(3, settings.trackedPackages.size)
        assertEquals(false, settings.includeGroups)
        assertEquals(false, settings.penaltyEnabled)
    }
}
