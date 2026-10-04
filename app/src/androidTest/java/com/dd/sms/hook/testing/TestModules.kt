package com.dd.sms.hook.testing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import com.dd.sms.hook.shared.data.db.AppDatabase
import com.dd.sms.hook.shared.data.di.ApplicationScope
import com.dd.sms.hook.shared.data.di.CoreModule
import com.dd.sms.hook.features.settings.data.repository.SettingsRepositoryImpl
import com.dd.sms.hook.features.settings.di.SettingsModule
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.UUID
import javax.inject.Singleton

/** In-memory database and the test WorkManager; every test gets a fresh graph. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CoreModule::class])
object TestCoreModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    /** Initialised by [WorkManagerTestRule] before anything asks for it. */
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}

/** A DataStore file per graph: two DataStores on one file in a process would throw. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SettingsModule::class])
abstract class TestSettingsModule {
    @Binds
    @Singleton
    abstract fun bindRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings-test-${UUID.randomUUID()}") }
    }
}
