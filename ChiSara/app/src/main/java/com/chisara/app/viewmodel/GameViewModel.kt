package com.chisara.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chisara.app.ChiSaraApp
import com.chisara.app.data.db.dao.ContactAccuracy
import com.chisara.app.data.db.entity.Contact
import com.chisara.app.data.db.entity.NotificationEvent
import com.chisara.app.data.repository.GameRepository
import com.chisara.app.notification.NotificationConfig
import com.chisara.app.permissions.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionState(
    val notificationAccess: Boolean = false,
    val usageAccess: Boolean = false,
    val postNotifications: Boolean = false
) {
    val allGranted: Boolean get() = notificationAccess && usageAccess && postNotifications
}

data class StatsUiState(
    val totalScore: Int = 0,
    val attempts: Int = 0,
    val correct: Int = 0,
    val accuracyByContact: List<ContactAccuracy> = emptyList()
) {
    val accuracyPercent: Int get() = if (attempts == 0) 0 else (correct * 100) / attempts
}

/** Fully-loaded data for the guess screen. */
data class GuessScreenState(
    val eventId: Long,
    val sourcePackage: String,
    val appName: String,
    val arrivalTs: Long,
    val recentContacts: List<Contact>
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository =
        (application as ChiSaraApp).repository

    private val _permissions = MutableStateFlow(PermissionState())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    val pendingEvents: StateFlow<List<NotificationEvent>> =
        repository.observePendingEvents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<StatsUiState> =
        combine(
            repository.observeTotalScore(),
            repository.observeTotalAttempts(),
            repository.observeCorrectCount(),
            repository.observeContactAccuracy(minAttempts = 1)
        ) { score, attempts, correct, accuracy ->
            StatsUiState(score, attempts, correct, accuracy)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    private val _guessScreen = MutableStateFlow<GuessScreenState?>(null)
    val guessScreen: StateFlow<GuessScreenState?> = _guessScreen.asStateFlow()

    private val _outcome = MutableStateFlow<GameRepository.GuessOutcome?>(null)
    val outcome: StateFlow<GameRepository.GuessOutcome?> = _outcome.asStateFlow()

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        _permissions.value = PermissionState(
            notificationAccess = PermissionUtils.hasNotificationAccess(ctx),
            usageAccess = PermissionUtils.hasUsageAccess(ctx),
            postNotifications = PermissionUtils.hasPostNotifications(ctx)
        )
    }

    fun loadEventForGuess(eventId: Long) {
        viewModelScope.launch {
            val event = repository.getEvent(eventId) ?: run {
                _guessScreen.value = null
                return@launch
            }
            val contacts = repository.recentContactsFor(event.sourcePackage)
            _guessScreen.value = GuessScreenState(
                eventId = event.id,
                sourcePackage = event.sourcePackage,
                appName = NotificationConfig.displayName(event.sourcePackage),
                arrivalTs = event.arrivalTs,
                recentContacts = contacts
            )
        }
    }

    fun submitGuess(eventId: Long, guessedName: String) {
        viewModelScope.launch {
            _outcome.value = repository.submitGuess(
                eventId = eventId,
                guessedName = guessedName,
                now = System.currentTimeMillis()
            )
        }
    }

    fun clearOutcome() {
        _outcome.value = null
        _guessScreen.value = null
    }
}
