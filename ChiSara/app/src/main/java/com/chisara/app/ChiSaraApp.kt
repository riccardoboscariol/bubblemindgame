package com.chisara.app

import android.app.Application
import com.chisara.app.data.repository.GameRepository

/**
 * Holds the single [GameRepository] so the notification service and the UI share
 * one instance (and one Room database).
 */
class ChiSaraApp : Application() {
    val repository: GameRepository by lazy { GameRepository(this) }
}
