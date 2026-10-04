package com.dd.sms.hook.features.dispatch.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dd.sms.hook.shared.domain.constants.DatabaseConstants

@Entity(tableName = DatabaseConstants.TABLE_RECEIVED_SMS, indices = [Index("received_at")])
data class ReceivedSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val sender: String,
    val body: String,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    @ColumnInfo(name = "subscription_id") val subscriptionId: Int,
    @ColumnInfo(name = "matched_count") val matchedCount: Int,
)
