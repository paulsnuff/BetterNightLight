package io.github.paulsnuff.betternightlight.ui

import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.text.TextUtils
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.paulsnuff.betternightlight.data.AppThemeMode
import io.github.paulsnuff.betternightlight.ui.navigation.SlidingBottomNavigationBar
import io.github.paulsnuff.betternightlight.ui.navigation.TopLevelDestination
import io.github.paulsnuff.betternightlight.ui.navigation.bottomBarNestedScroll
import io.github.paulsnuff.betternightlight.ui.navigation.rememberBottomBarHideController
import io.github.paulsnuff.betternightlight.ui.screens.about.AboutRoute
import io.github.paulsnuff.betternightlight.ui.screens.home.HomeRoute
import io.github.paulsnuff.betternightlight.ui.screens.settings.AppViewModel
import io.github.paulsnuff.betternightlight.ui.screens.settings.SettingsRoute
import io.github.paulsnuff.betternightlight.ui.theme.BetterNightLightTheme
import kotlinx.coroutines.launch
import java.util.Locale

private const val NAV_ANIMATION_MS = 350

@Composable
fun AppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentConfiguration = LocalConfiguration.current

    val currentLocale =
        remember(currentConfiguration) {
            if (!currentConfiguration.locales.isEmpty) {
                currentConfiguration.locales.get(0)
            } else {
                Locale.getDefault()
            }
        }

    val localizedConfiguration =
        remember(currentLocale, currentConfiguration) {
            Configuration(currentConfiguration).apply {
                setLocale(currentLocale)
                setLayoutDirection(currentLocale)
            }
        }

    val localizedContext =
        remember(currentLocale, context, localizedConfiguration) {
            val configContext = context.createConfigurationContext(localizedConfiguration)
            object : ContextWrapper(context) {
                override fun getResources(): Resources = configContext.resources

                override fun getAssets(): AssetManager = configContext.assets
            }
        }

    LaunchedEffect(currentLocale) {
        viewModel.refreshLanguageFromSystem()
    }

    val layoutDirection =
        remember(currentLocale) {
            if (TextUtils.getLayoutDirectionFromLocale(currentLocale) == View.LAYOUT_DIRECTION_RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
        }

    val isDark =
        when (uiState.themeMode) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalLayoutDirection provides layoutDirection,
    ) {
        BetterNightLightTheme(
            darkTheme = isDark,
            dynamicColor = uiState.dynamicColor,
        ) {
            BetterNightLightApp(
                viewModel = viewModel,
            )
        }
    }
}

@Composable
fun BetterNightLightApp(viewModel: AppViewModel) {
    val destinations = remember { TopLevelDestination.entries }
    val homeIndex =
        remember(destinations) {
            destinations.indexOf(TopLevelDestination.HOME).coerceAtLeast(0)
        }
    val pagerState =
        rememberPagerState(
            initialPage = homeIndex,
            pageCount = { destinations.size },
        )
    val coroutineScope = rememberCoroutineScope()
    val bottomBarHideController = rememberBottomBarHideController(pagerState = pagerState)

    fun scrollTo(target: Int) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = target,
                animationSpec =
                    tween(
                        durationMillis = NAV_ANIMATION_MS,
                        easing = FastOutSlowInEasing,
                    ),
            )
        }
    }

    BackHandler(enabled = pagerState.currentPage != homeIndex) {
        scrollTo(homeIndex)
    }

    LaunchedEffect(homeIndex) {
        viewModel.permissionMissing.collect {
            scrollTo(homeIndex)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            SlidingBottomNavigationBar(
                pagerState = pagerState,
                destinations = destinations,
                hideFraction = { bottomBarHideController.hideFraction },
                onDestinationClick = { index -> scrollTo(index) },
                modifier = Modifier.zIndex(1f),
            )
        },
    ) { innerPadding ->
        val contentBottomPadding = innerPadding.calculateBottomPadding()

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier =
                Modifier
                    .fillMaxSize()
                    .bottomBarNestedScroll(bottomBarHideController)
                    .padding(top = innerPadding.calculateTopPadding()),
        ) { pageIndex ->
            when (destinations[pageIndex]) {
                TopLevelDestination.HOME -> {
                    HomeRoute(
                        contentBottomPadding = contentBottomPadding,
                    )
                }

                TopLevelDestination.SETTINGS -> {
                    SettingsRoute(
                        viewModel = viewModel,
                        contentBottomPadding = contentBottomPadding,
                    )
                }

                TopLevelDestination.ABOUT -> {
                    AboutRoute(
                        contentBottomPadding = contentBottomPadding,
                    )
                }
            }
        }
    }
}
