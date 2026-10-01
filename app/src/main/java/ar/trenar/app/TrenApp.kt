package ar.trenar.app

import android.app.Application
import ar.trenar.app.di.ServiceLocator
import ar.trenar.app.notifications.DelayWorker
import ar.trenar.app.notifications.NotificationHelper

class TrenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NotificationHelper.ensureChannels(this)
        DelayWorker.schedule(this)
    }
}
