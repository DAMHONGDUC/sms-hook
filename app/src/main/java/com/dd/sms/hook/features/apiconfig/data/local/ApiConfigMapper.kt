package com.dd.sms.hook.features.apiconfig.data.local

import com.dd.sms.hook.shared.data.serialization.AppJson
import com.dd.sms.hook.shared.data.serialization.NameValueDto
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.model.HeaderEntry
import com.dd.sms.hook.features.apiconfig.domain.model.HttpMethod
import com.dd.sms.hook.features.apiconfig.domain.model.MatchMode
import com.dd.sms.hook.features.apiconfig.domain.model.SmsFilter

object ApiConfigMapper {
    fun toDomain(entity: ApiConfigEntity): ApiConfig = ApiConfig(
        id = entity.id,
        name = entity.name,
        url = entity.url,
        method = HttpMethod.entries.firstOrNull { it.name == entity.method } ?: HttpMethod.POST,
        headers = AppJson.decodePairs(entity.headersJson).map { HeaderEntry(it.name, it.value) },
        bodyTemplate = entity.bodyTemplate,
        filter = SmsFilter(
            senders = entity.senderFilter,
            keyword = entity.keywordFilter,
            mode = MatchMode.entries.firstOrNull { it.name == entity.matchMode } ?: MatchMode.CONTAINS,
        ),
        enabled = entity.enabled,
        timeoutSeconds = entity.timeoutSeconds,
        maxRetries = entity.maxRetries,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    fun toEntity(config: ApiConfig): ApiConfigEntity = ApiConfigEntity(
        id = config.id,
        name = config.name,
        url = config.url,
        method = config.method.name,
        headersJson = AppJson.encodePairs(config.headers.map { NameValueDto(it.name, it.value) }),
        bodyTemplate = config.bodyTemplate,
        senderFilter = config.filter.senders,
        keywordFilter = config.filter.keyword,
        matchMode = config.filter.mode.name,
        enabled = config.enabled,
        timeoutSeconds = config.timeoutSeconds,
        maxRetries = config.maxRetries,
        createdAt = config.createdAt,
        updatedAt = config.updatedAt,
    )
}
