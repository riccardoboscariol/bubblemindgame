package com.chisara.app

import android.app.Application
import com.chisara.app.data.repository.GameRepository
import com.chisara.app.data.settings.SettingsRepository

/**
 * Holds the shared singletons so the notification service and the UI share one
 * [GameRepository] (and one Room database) and one [SettingsRepository].
 */
class ChiSaraApp : Application() {
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val repository: GameRepository by lazy { GameRepository(this) }
}
