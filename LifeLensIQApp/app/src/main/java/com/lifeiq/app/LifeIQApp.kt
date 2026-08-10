package com.lifeiq.app

import android.app.Application
import com.lifeiq.app.di.ServiceLocator

class LifeIQApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
