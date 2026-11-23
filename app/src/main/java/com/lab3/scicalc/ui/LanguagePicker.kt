package com.lab3.scicalc.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lab3.scicalc.R
import timber.log.Timber

/**
 * In-app language switcher built on the per-app language API
 * (AppCompatDelegate.setApplicationLocales). Works on all API levels:
 * Android 13+ uses the system setting, older versions are handled by AppCompat
 * (see AppLocalesMetadataHolderService in the manifest).
 *
 * To add a language: create res/values-xx/strings.xml and add "xx"
 * to language_tags / language_names and to res/xml/locales_config.xml.
 */
object LanguagePicker {

    fun show(context: Context) {
        val names = arrayOf(context.getString(R.string.language_system)) +
            context.resources.getStringArray(R.array.language_names)
        val tags = arrayOf("") + context.resources.getStringArray(R.array.language_tags)

        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val checked = tags.indexOfFirst { it.isNotEmpty() && current.startsWith(it) }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.language_dialog_title)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                dialog.dismiss()
                apply(tags[which])
            }
            .show()
    }

    private fun apply(tag: String) {
        Timber.i("Language changed to '%s'", tag.ifEmpty { "system" })
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        // Activities are recreated automatically; their state is kept via onSaveInstanceState.
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
