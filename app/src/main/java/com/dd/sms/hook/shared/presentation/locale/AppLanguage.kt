package com.dd.sms.hook.shared.presentation.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dd.sms.hook.shared.domain.logging.AppLogger
import java.util.Locale

private const val TAG = "AppLanguage"

/**
 * Languages the app ships strings for. [tag] null means "follow the device".
 * [nativeName] is the language's own name, identical in every locale, so it is not translated.
 */
enum class AppLanguage(val tag: String?, val nativeName: String?) {
    SYSTEM(null, null),
    ENGLISH("en", "English"),
    VIETNAMESE("vi", "Tiếng Việt"),
    CHINESE("zh-CN", "简体中文"),
    SPANISH("es", "Español"),
    HINDI("hi", "हिन्दी"),
    ARABIC("ar", "العربية"),
}

/** Reads and changes the per-app language (Android 13+ system setting, AppCompat storage below). */
object LocaleController {
    fun current(): AppLanguage {
        val locales: LocaleListCompat = AppCompatDelegate.getApplicationLocales()
        val locale: Locale = locales[0] ?: return AppLanguage.SYSTEM

        return AppLanguage.entries.firstOrNull { it.tag != null && Locale.forLanguageTag(it.tag).language == locale.language }
            ?: AppLanguage.SYSTEM
    }

    /** Recreates the activity with the new strings. */
    fun apply(language: AppLanguage) {
        val locales: LocaleListCompat = language.tag
            ?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()

        AppCompatDelegate.setApplicationLocales(locales)
        AppLogger.i(TAG, "language changed - {language: $language, tag: ${language.tag}}")
    }
}
