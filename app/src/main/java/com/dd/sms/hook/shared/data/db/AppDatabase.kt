package com.dd.sms.hook.shared.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dd.sms.hook.features.apiconfig.data.local.ApiConfigDao
import com.dd.sms.hook.features.apiconfig.data.local.ApiConfigEntity
import com.dd.sms.hook.features.calllog.data.local.CallLogDao
import com.dd.sms.hook.features.calllog.data.local.CallLogEntity
import com.dd.sms.hook.features.dispatch.data.local.ReceivedSmsDao
import com.dd.sms.hook.features.dispatch.data.local.ReceivedSmsEntity

/** Composes the feature-owned tables. Bump [version] and add a migration for any schema change. */
@Database(
    entities = [ApiConfigEntity::class, CallLogEntity::class, ReceivedSmsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apiConfigDao(): ApiConfigDao

    abstract fun callLogDao(): CallLogDao

    abstract fun receivedSmsDao(): ReceivedSmsDao
}
