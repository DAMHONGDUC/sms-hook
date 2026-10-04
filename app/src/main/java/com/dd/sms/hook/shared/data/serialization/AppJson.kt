package com.dd.sms.hook.shared.data.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** A header or other name/value pair as stored in a JSON column. */
@Serializable
data class NameValueDto(val name: String, val value: String)

/** The one Json instance used for DB columns and body templating. */
object AppJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val pairListSerializer = ListSerializer(NameValueDto.serializer())

    fun encodePairs(pairs: List<NameValueDto>): String = json.encodeToString(pairListSerializer, pairs)

    fun decodePairs(raw: String): List<NameValueDto> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(pairListSerializer, raw)
}
