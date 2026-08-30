package com.sadam.thumbtype.mobile

import android.app.Application
import com.sadam.thumbtype.mobile.data.repository.DefaultThumbTypeRepository
import com.sadam.thumbtype.mobile.data.repository.ThumbTypeRepository

/**
 * Application-level dependency boundary.
 *
 * This intentionally avoids a DI framework for now. The container gives ViewModels a
 * stable dependency source while keeping Android construction details out of feature code.
 * Hilt/Koin can replace this later without changing the repository contract or screens.
 */
class ThumbTypeApplication : Application() {
    lateinit var container: ThumbTypeAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = ThumbTypeAppContainer(this)
    }
}

class ThumbTypeAppContainer(application: Application) {
    val repository: ThumbTypeRepository = DefaultThumbTypeRepository(application.applicationContext)
}
