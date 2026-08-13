package com.lifelensiq.app

import android.app.Application
import com.lifelensiq.app.di.ServiceLocator

class LifeLensIQApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
