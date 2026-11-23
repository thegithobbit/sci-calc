package com.lab3.scicalc

import android.app.Application
import com.lab3.scicalc.logging.DebugLogTree
import com.lab3.scicalc.logging.ReleaseTree
import timber.log.Timber

/**
 * Application entry point. The logger is configured once here,
 * depending on the build type:
 *  - debug   -> every level, tag contains file:line
 *  - release -> INFO and above only
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) DebugLogTree() else ReleaseTree())
        Timber.i("Application started, version %s (debug=%b)", BuildConfig.VERSION_NAME, BuildConfig.DEBUG)
    }
}
