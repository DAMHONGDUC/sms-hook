package com.dd.sms.hook.features.calllog.di

import com.dd.sms.hook.shared.data.db.AppDatabase
import com.dd.sms.hook.features.calllog.data.local.CallLogDao
import com.dd.sms.hook.features.calllog.data.repository.CallLogRepositoryImpl
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CallLogModule {
    @Binds
    @Singleton
    abstract fun bindRepository(impl: CallLogRepositoryImpl): CallLogRepository

    companion object {
        @Provides
        fun provideDao(db: AppDatabase): CallLogDao = db.callLogDao()
    }
}
