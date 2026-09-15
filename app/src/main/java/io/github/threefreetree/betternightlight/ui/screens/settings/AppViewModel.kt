package io.github.threefreetree.betternightlight.ui.screens.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.threefreetree.betternightlight.data.AppLanguage
import io.github.threefreetree.betternightlight.data.AppThemeMode
import io.github.threefreetree.betternightlight.data.PermissionGate
import io.github.threefreetree.betternightlight.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val language: AppLanguage = AppLanguage.AUTO,
)

@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        private val preferencesRepository: UserPreferencesRepository,
        permissionGate: PermissionGate,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val uiState: StateFlow<AppUiState> =
            preferencesRepository.userPreferencesFlow
                .map { prefs ->
                    AppUiState(
                        themeMode = prefs.themeMode,
                        dynamicColor = prefs.dynamicColor,
                        language = prefs.language,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AppUiState(),
                )

        val permissionMissing: Flow<Unit> = permissionGate.permissionMissing

        private fun currentAppLanguage(): AppLanguage {
            val appLocales = currentAppLocales()
            if (appLocales.isEmpty) return AppLanguage.AUTO
            val locale = appLocales.get(0) ?: return AppLanguage.AUTO
            return when (locale.language.lowercase()) {
                "en" -> AppLanguage.ENGLISH
                "pl" -> AppLanguage.POLISH
                else -> AppLanguage.AUTO
            }
        }

        private fun currentAppLocales(): LocaleListCompat {
            val frameworkLocales =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    (context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager)?.applicationLocales
                } else {
                    null
                }
            if (frameworkLocales != null && !frameworkLocales.isEmpty) {
                return LocaleListCompat.forLanguageTags(frameworkLocales.toLanguageTags())
            }
            return AppCompatDelegate.getApplicationLocales()
        }

        fun refreshLanguageFromSystem() {
            viewModelScope.launch {
                val synced = currentAppLanguage()
                val current = preferencesRepository.userPreferencesFlow.first()
                if (synced != current.language) {
                    preferencesRepository.updateLanguage(synced)
                }
            }
        }

        fun updateThemeMode(themeMode: AppThemeMode) {
            viewModelScope.launch {
                preferencesRepository.updateThemeMode(themeMode)
            }
        }

        fun updateDynamicColor(dynamicColor: Boolean) {
            viewModelScope.launch {
                preferencesRepository.updateDynamicColor(dynamicColor)
            }
        }

        fun updateLanguage(language: AppLanguage) {
            viewModelScope.launch {
                preferencesRepository.updateLanguage(language)
                val localeList =
                    if (language.code != null) {
                        LocaleListCompat.forLanguageTags(language.code)
                    } else {
                        LocaleListCompat.getEmptyLocaleList()
                    }
                AppCompatDelegate.setApplicationLocales(localeList)
            }
        }
    }
