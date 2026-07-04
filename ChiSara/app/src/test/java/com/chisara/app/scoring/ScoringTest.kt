package com.chisara.app.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    @Test
    fun wrongAnswer_scoresZeroByDefault() {
        val score = Scoring.score(
            correct = false,
            contactMessageCount = 1,
            totalMessagesInApp = 100,
            distinctContacts = 10
        )
        assertEquals(0, score)
    }

    @Test
    fun wrongAnswer_appliesConfiguredPenalty() {
        val score = Scoring.score(
            correct = false,
            contactMessageCount = 1,
            totalMessagesInApp = 100,
            distinctContacts = 10,
            config = Scoring.Config(wrongAnswerPenalty = -5)
        )
        assertEquals(-5, score)
    }

    @Test
    fun rareContact_scoresHigherThanFrequentContact() {
        // Same app history; rare contact sent 1 of ~100, frequent one sent 80 of ~100.
        val rare = Scoring.score(
            correct = true,
            contactMessageCount = 1,
            totalMessagesInApp = 100,
            distinctContacts = 12
        )
        val frequent = Scoring.score(
            correct = true,
            contactMessageCount = 80,
            totalMessagesInApp = 100,
            distinctContacts = 12
        )
        assertTrue("rare ($rare) should beat frequent ($frequent)", rare > frequent)
    }

    @Test
    fun priorProbability_isSmoothedNeverZero() {
        // Brand-new contact, empty history: still finite, positive, <= 1.
        val p = Scoring.priorProbability(
            contactMessageCount = 0,
            totalMessagesInApp = 0,
            distinctContacts = 0
        )
        assertTrue(p > 0.0)
        assertTrue(p <= 1.0)
    }

    @Test
    fun correctScore_isCappedAtMax() {
        // An absurdly rare contact would otherwise produce a huge score.
        val score = Scoring.scoreForCorrect(
            p = 1e-9,
            config = Scoring.Config(scale = 10.0, maxScore = 100)
        )
        assertEquals(100, score)
    }

    @Test
    fun selfInformation_halfProbabilityIsOneBit() {
        assertEquals(1.0, Scoring.selfInformationBits(0.5), 1e-9)
    }

    @Test
    fun score_forFiftyFiftyContact_matchesFormula() {
        // p smoothed = (49+1)/(100+2) ~= 0.4902 -> bits ~= 1.028 -> *10 -> 10
        val score = Scoring.score(
            correct = true,
            contactMessageCount = 49,
            totalMessagesInApp = 100,
            distinctContacts = 2
        )
        assertEquals(10, score)
    }
}
