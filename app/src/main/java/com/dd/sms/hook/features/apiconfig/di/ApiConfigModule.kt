package com.dd.sms.hook.features.apiconfig.di

import com.dd.sms.hook.shared.data.db.AppDatabase
import com.dd.sms.hook.features.apiconfig.data.local.ApiConfigDao
import com.dd.sms.hook.features.apiconfig.data.repository.ApiConfigRepositoryImpl
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiConfigModule {
    @Binds
    @Singleton
    abstract fun bindRepository(impl: ApiConfigRepositoryImpl): ApiConfigRepository

    companion object {
        @Provides
        fun provideDao(db: AppDatabase): ApiConfigDao = db.apiConfigDao()
    }
}
