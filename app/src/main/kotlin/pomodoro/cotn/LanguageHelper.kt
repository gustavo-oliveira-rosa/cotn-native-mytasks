package pomodoro.cotn

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

object LanguageHelper {

    private const val PREFS = "mytasks"
    private const val KEY = "lang"

    const val LANG_PT = "pt"
    const val LANG_EN = "en"
    const val LANG_ES = "es"

    fun isSelected(context: Context): Boolean =
        getPrefs(context).contains(KEY)

    fun getSaved(context: Context): String? =
        getPrefs(context).getString(KEY, null)

    fun save(context: Context, lang: String) {
        getPrefs(context).edit().putString(KEY, lang).apply()
        applyLocale(localeFrom(lang), context.resources)
    }

    fun applyLocale(context: Context): Context {
        val lang = getSaved(context) ?: return context
        return applyLocale(localeFrom(lang), context)
    }

    private fun localeFrom(lang: String): Locale = Locale(lang)

    private fun applyLocale(locale: Locale, context: Context): Context {
        Locale.setDefault(locale)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            applyLocale(locale, context.resources)
            context
        }
    }

    private fun applyLocale(locale: Locale, resources: android.content.res.Resources) {
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun showLanguagePicker(activity: AppCompatActivity) {
        val names: Array<CharSequence> = arrayOf("Português", "English", "Español")
        val codes = arrayOf(LANG_PT, LANG_EN, LANG_ES)
        val current = activity.resources.configuration.locale.language
        var checked = codes.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.choose_language)
            .setSingleChoiceItems(names, checked) { _, which -> checked = which }
            .setPositiveButton(R.string.select) { _, _ ->
                if (checked in codes.indices) {
                    save(activity, codes[checked])
                    activity.recreate()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}