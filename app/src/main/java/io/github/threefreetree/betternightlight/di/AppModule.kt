package io.github.threefreetree.betternightlight.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.threefreetree.betternightlight.data.DeviceLocationProviderImpl
import io.github.threefreetree.betternightlight.data.NightLightOverrideRepository
import io.github.threefreetree.betternightlight.data.NightLightOverrideRepositoryImpl
import io.github.threefreetree.betternightlight.data.NightLightReader
import io.github.threefreetree.betternightlight.data.NightLightRepository
import io.github.threefreetree.betternightlight.data.NightLightRepositoryImpl
import io.github.threefreetree.betternightlight.data.SettingsSecureNightLightController
import io.github.threefreetree.betternightlight.data.UserPreferencesRepository
import io.github.threefreetree.betternightlight.data.UserPreferencesRepositoryImpl
import io.github.threefreetree.betternightlight.domain.AutomationWorkScheduler
import io.github.threefreetree.betternightlight.domain.DefaultDispatcherProvider
import io.github.threefreetree.betternightlight.domain.DeviceLocationProvider
import io.github.threefreetree.betternightlight.domain.DispatcherProvider
import io.github.threefreetree.betternightlight.domain.NightLightController
import io.github.threefreetree.betternightlight.service.WorkManagerAutomationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideNightLightReader(
        @ApplicationContext context: Context,
    ): NightLightReader = NightLightReader(context)

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userPreferencesDataStore

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNightLightRepository(impl: NightLightRepositoryImpl): NightLightRepository

    @Binds
    @Singleton
    abstract fun bindNightLightController(impl: SettingsSecureNightLightController): NightLightController

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindNightLightOverrideRepository(impl: NightLightOverrideRepositoryImpl): NightLightOverrideRepository

    @Binds
    @Singleton
    abstract fun bindDeviceLocationProvider(impl: DeviceLocationProviderImpl): DeviceLocationProvider

    @Binds
    @Singleton
    abstract fun bindAutomationWorkScheduler(impl: WorkManagerAutomationScheduler): AutomationWorkScheduler
}
