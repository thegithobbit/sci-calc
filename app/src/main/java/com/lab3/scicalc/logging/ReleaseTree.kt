package com.lab3.scicalc.logging

import android.util.Log
import timber.log.Timber

/**
 * Logging tree for release builds.
 * Verbose and debug messages are dropped (they may contain user input),
 * only INFO and above go to Logcat under a fixed tag.
 */
class ReleaseTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val finalTag = tag ?: DEFAULT_TAG
        Log.println(priority, finalTag, message)
        if (t != null && priority >= Log.ERROR) {
            Log.println(priority, finalTag, Log.getStackTraceString(t))
        }
    }

    private companion object {
        const val DEFAULT_TAG = "SciCalc"
    }
}
