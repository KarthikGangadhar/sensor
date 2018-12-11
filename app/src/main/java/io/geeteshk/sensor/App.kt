package io.geeteshk.sensor

import android.app.Application
import com.google.firebase.FirebaseApp
import com.polidea.rxandroidble2.RxBleClient
import com.squareup.leakcanary.LeakCanary
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Timber.plant(Timber.DebugTree())
        bleClient = RxBleClient.create(this)
        if (LeakCanary.isInAnalyzerProcess(this)) return
        LeakCanary.install(this)
    }

    companion object {
        lateinit var bleClient: RxBleClient
            private set
    }
}