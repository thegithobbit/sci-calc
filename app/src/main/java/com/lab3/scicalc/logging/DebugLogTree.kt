package com.lab3.scicalc.logging

import timber.log.Timber

/**
 * Debug tree: logs everything and adds the file name and line number to the tag,
 * so a Logcat line like `SciCalc/MainActivity.kt:42` can be clicked in Android Studio.
 */
class DebugLogTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String =
        "SciCalc/${element.fileName}:${element.lineNumber}"
}
