import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    fun applyPersistedLocale(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For API 33+, setApplicationLocales handles persistence and context updates.
            // This is the preferred method.
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        } else {
            // For older versions, we manually update the configuration for the current context.
            // The persistence is handled by our DataStore.
            // The actual application of this locale consistently happens in attachBaseContext.
            updateResourcesLegacy(context, languageCode)
        }
    }

    fun wrapContext(context: Context, languageCode: String): ContextWrapper {
        val locale = Locale(languageCode)
        Locale.setDefault(locale) // Set default locale for the app process

        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            configuration.setLocale(locale)
            configuration.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }

        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)

        return ContextWrapper(context.createConfigurationContext(configuration))
    }

    // This function is for legacy versions if you need to update resources immediately
    // without full activity recreation (less common now with setApplicationLocales).
    // Primarily, context wrapping in attachBaseContext is the key.
    private fun updateResourcesLegacy(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val res = context.resources
        val config = Configuration(res.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
    }

    // Utility to get the current persisted language before full DI might be ready in attachBaseContext
    // This is a simplified example; for robustness, LanguageRepository.selectedLanguageFlow should be preferred.
    // For attachBaseContext, you might need a synchronous way if DI isn't fully setup.
    // However, with setApplicationLocales on API 33+, this becomes less of an issue for initial load.
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
}