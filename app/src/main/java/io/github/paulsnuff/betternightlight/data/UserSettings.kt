package io.github.paulsnuff.betternightlight.data

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage(
    val code: String?,
) {
    AUTO(null),
    ENGLISH("en"),
    POLISH("pl"),
}
