package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.icu.text.Collator
import android.icu.util.ULocale
import android.os.LocaleList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Languages the UI is translated into. Each needs a matching `res/values-<qualifier>/strings.xml`
 * (`values-pt-rBR`, `values-zh-rCN`, `values-nb`, and `values-in` for Indonesian).
 * [tag] is the BCP 47 tag that gets saved, [code] the two letters on the language button, and
 * [displayName] the language's own name for itself, so it is recognizable whatever the UI is in.
 */
enum class AppLanguage(val tag: String, val code: String, val displayName: String) {
    CS("cs", "CS", "Čeština"),
    DE("de", "DE", "Deutsch"),
    EN("en", "EN", "English"),
    ES("es", "ES", "Español"),
    FI("fi", "FI", "Suomi"),
    FR("fr", "FR", "Français"),
    HI("hi", "HI", "हिन्दी"),
    HU("hu", "HU", "Magyar"),
    ID("id", "ID", "Bahasa Indonesia"),
    IT("it", "IT", "Italiano"),
    JA("ja", "JA", "日本語"),
    KO("ko", "KO", "한국어"),
    NB("nb", "NO", "Norsk bokmål"),
    NL("nl", "NL", "Nederlands"),
    PL("pl", "PL", "Polski"),
    // Both are "PT"; the country code is what tells them apart on the button.
    PT_BR("pt-BR", "BR", "Português (Brasil)"),
    PT_PT("pt-PT", "PT", "Português (Portugal)"),
    RU("ru", "RU", "Русский"),
    SL("sl", "SL", "Slovenščina"),
    SV("sv", "SV", "Svenska"),
    TH("th", "TH", "ไทย"),
    TR("tr", "TR", "Türkçe"),
    UK("uk", "UK", "Українська"),
    ZH_CN("zh-CN", "ZH", "简体中文");

    companion object {
        val DEFAULT = EN

        /**
         * Sorted by name, the order the language menu lists them in. A collator rather than
         * plain string order, so "Čeština" sorts under C instead of after every Latin name.
         */
        val sorted: List<AppLanguage> = Collator.getInstance(Locale.ROOT).let { collator ->
            entries.sortedWith { a, b -> collator.compare(a.displayName, b.displayName) }
        }

        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /**
         * The translation Android would pick for a device locale, or null if there is none.
         * Likely subtags fill in what the locale leaves out: plain "pt" means Brazil, and
         * "zh-TW" is Traditional script, which the Simplified translation doesn't cover.
         */
        fun fromLocale(locale: Locale): AppLanguage? {
            val full = ULocale.addLikelySubtags(ULocale.forLocale(locale))
            return when (full.language) {
                "pt" -> if (full.country == "BR") PT_BR else PT_PT
                "zh" -> if (full.script == "Hans") ZH_CN else null
                "nb", "no" -> NB
                "id", "in" -> ID
                else -> entries.firstOrNull { it.tag == full.language }
            }
        }

        /**
         * The language the UI is in: the user's pick, else the first device language we have
         * a translation for, else English. Mirrors how Android resolves resources when no
         * override is applied, so the button always names the language actually shown.
         */
        fun resolve(savedTag: String?): AppLanguage {
            fromTag(savedTag)?.let { return it }
            // The system configuration, not Locale.getDefault(), which the app can change.
            val deviceLocales = Resources.getSystem().configuration.locales
            for (i in 0 until deviceLocales.size()) {
                fromLocale(deviceLocales[i])?.let { return it }
            }
            return DEFAULT
        }
    }
}

/**
 * Reads the saved language tag synchronously. Only for `attachBaseContext`, which has to
 * decide the locale before the Activity exists; the read is a single small file.
 */
fun readSavedLanguageBlocking(context: Context): String? = runCatching {
    runBlocking { context.applicationContext.dataStore.data.first()[SettingsKeys.language] }
}.getOrNull()

/**
 * A copy of this context whose resources use [tag]'s locale. A null or unknown tag leaves the
 * context untouched, so Android picks the best match from the device languages as usual.
 */
fun Context.withLanguage(tag: String?): Context {
    val language = AppLanguage.fromTag(tag) ?: return this
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList(Locale.forLanguageTag(language.tag)))
    return createConfigurationContext(config)
}
