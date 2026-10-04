package com.dd.sms.hook.features.calllog.data.local

import com.dd.sms.hook.shared.data.serialization.AppJson
import com.dd.sms.hook.shared.data.serialization.NameValueDto
import com.dd.sms.hook.features.apiconfig.domain.model.HeaderEntry
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger

object CallLogMapper {
    fun toDomain(entity: CallLogEntity): CallLog = CallLog(
        id = entity.id,
        configId = entity.configId,
        configName = entity.configName,
        smsId = entity.smsId,
        smsSender = entity.smsSender,
        smsBody = entity.smsBody,
        url = entity.url,
        method = entity.method,
        requestHeaders = AppJson.decodePairs(entity.requestHeadersJson).map { HeaderEntry(it.name, it.value) },
        requestBody = entity.requestBody,
        responseCode = entity.responseCode,
        responseBody = entity.responseBody,
        errorMessage = entity.errorMessage,
        durationMs = entity.durationMs,
        attempt = entity.attempt,
        status = if (entity.status == CallStatus.SUCCESS.name) CallStatus.SUCCESS else CallStatus.FAILED,
        trigger = CallTrigger.entries.firstOrNull { it.name == entity.trigger } ?: CallTrigger.SMS,
        createdAt = entity.createdAt,
    )

    fun toEntity(log: CallLog): CallLogEntity = CallLogEntity(
        id = log.id,
        configId = log.configId,
        configName = log.configName,
        smsId = log.smsId,
        smsSender = log.smsSender,
        smsBody = log.smsBody,
        url = log.url,
        method = log.method,
        requestHeadersJson = AppJson.encodePairs(log.requestHeaders.map { NameValueDto(it.name, it.value) }),
        requestBody = log.requestBody,
        responseCode = log.responseCode,
        responseBody = log.responseBody,
        errorMessage = log.errorMessage,
        durationMs = log.durationMs,
        attempt = log.attempt,
        status = log.status.name,
        trigger = log.trigger.name,
        createdAt = log.createdAt,
    )
}
