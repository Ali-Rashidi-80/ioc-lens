package io.github.mrcoder20.ioclens.core.localization

/**
 * Polymorphic number formatting extensions for [LocalizationProvider].
 *
 * Expands [formatNumber] to accept any [Number] (Int, Long, Double, Float, Short, Byte),
 * converting digits 0-9 to Persian numerals ۰-۹ when [lang] == [AppLanguage.PERSIAN],
 * and standard digits when [lang] == [AppLanguage.ENGLISH] or [AppLanguage.RUSSIAN].
 *
 * Backward compatibility for `formatNumber(number: Int, lang: AppLanguage)` is preserved
 * by the existing member function on [LocalizationProvider].
 */
fun LocalizationProvider.formatNumber(number: Number, lang: AppLanguage): String {
    return if (lang == AppLanguage.PERSIAN) {
        toPersianDigits(number.toString())
    } else {
        number.toString()
    }
}
