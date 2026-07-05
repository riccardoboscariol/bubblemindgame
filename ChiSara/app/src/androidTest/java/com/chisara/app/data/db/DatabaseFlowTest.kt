package com.chisara.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chisara.app.data.db.entity.Contact
import com.chisara.app.data.db.entity.EventStatus
import com.chisara.app.data.db.entity.GuessAttempt
import com.chisara.app.data.db.entity.NotificationEvent
import com.chisara.app.notification.NotificationConfig
import com.chisara.app.scoring.Scoring
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * In-memory Room test exercising the intercept → guess → score data flow end to end
 * through the DAOs (no Android notification/usage services involved).
 */
@RunWith(AndroidJUnit4::class)
class DatabaseFlowTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun intercept_then_guess_producesScoredAttempt() = runBlocking {
        val pkg = NotificationConfig.PKG_TELEGRAM
        val contacts = db.contactDao()
        val events = db.notificationEventDao()
        val guesses = db.guessAttemptDao()

        // Simulate a few historical messages so the prior is meaningful.
        val rareId = contacts.insert(Contact(displayName = "Zia Pina", sourcePackage = pkg))
        val chattyId = contacts.insert(Contact(displayName = "Best Friend", sourcePackage = pkg))
        repeat(1) { contacts.incrementMessageCount(rareId, ts = 1000) }
        repeat(20) { contacts.incrementMessageCount(chattyId, ts = 1000) }

        // Intercept a new message from the rare contact.
        val eventId = events.insert(
            NotificationEvent(
                realContactId = rareId,
                realSenderName = "Zia Pina",
                sourcePackage = pkg,
                arrivalTs = 2000,
                replacementNotificationId = 1
            )
        )
        contacts.incrementMessageCount(rareId, ts = 2000)

        // Score a correct guess using the same prior the repository would compute.
        val count = contacts.findById(rareId)!!.historicalMessageCount
        val total = contacts.totalMessagesForPackage(pkg)
        val distinct = contacts.distinctContactsForPackage(pkg)
        val score = Scoring.score(correct = true, count, total, distinct)

        events.update(events.findById(eventId)!!.copy(status = EventStatus.VALID, responseTs = 3000))
        guesses.insert(
            GuessAttempt(
                eventId = eventId,
                guessedContactName = "Zia Pina",
                correct = true,
                scoreAwarded = score,
                createdTs = 3000
            )
        )

        assertEquals(2, distinct)
        assertTrue("rare contact should score points", score > 0)
        assertEquals(0, events.listByStatus(EventStatus.PENDING).size)

        val stored = guesses.findByEvent(eventId)!!
        assertTrue(stored.correct)
        assertEquals(score, stored.scoreAwarded)
    }

    @Test
    fun contactPrior_updatesAsMessagesArrive() = runBlocking {
        val pkg = NotificationConfig.PKG_WHATSAPP
        val contacts = db.contactDao()
        val id = contacts.insert(Contact(displayName = "Mario", sourcePackage = pkg))

        assertEquals(0L, contacts.findById(id)!!.historicalMessageCount)
        contacts.incrementMessageCount(id, ts = 10)
        contacts.incrementMessageCount(id, ts = 20)
        assertEquals(2L, contacts.findById(id)!!.historicalMessageCount)
        assertEquals(2L, contacts.totalMessagesForPackage(pkg))
    }
}
