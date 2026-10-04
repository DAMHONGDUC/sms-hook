package com.dd.sms.hook.features.apiconfig.domain.usecase

import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import com.dd.sms.hook.features.apiconfig.domain.service.ApiConfigError
import com.dd.sms.hook.features.apiconfig.domain.service.ApiConfigValidator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val TAG = "ApiConfigUseCases"

class ObserveApiConfigsUseCase @Inject constructor(private val repository: ApiConfigRepository) {
    operator fun invoke(): Flow<List<ApiConfig>> = repository.observeAll()
}

class ObserveEnabledApiCountUseCase @Inject constructor(private val repository: ApiConfigRepository) {
    operator fun invoke(): Flow<Int> = repository.observeEnabledCount()
}

class GetApiConfigUseCase @Inject constructor(private val repository: ApiConfigRepository) {
    suspend operator fun invoke(id: Long): ApiConfig? = repository.getById(id)
}

sealed interface SaveApiConfigResult {
    data class Saved(val id: Long) : SaveApiConfigResult
    data class Invalid(val errors: Set<ApiConfigError>) : SaveApiConfigResult
}

class SaveApiConfigUseCase @Inject constructor(
    private val repository: ApiConfigRepository,
    private val validator: ApiConfigValidator,
) {
    suspend operator fun invoke(config: ApiConfig): SaveApiConfigResult {
        val errors: Set<ApiConfigError> = validator.validate(config)
        val now: Long = TimeUtils.now()

        if (errors.isNotEmpty()) {
            AppLogger.i(TAG, "save rejected - {id: ${config.id}, errors: $errors}")
            return SaveApiConfigResult.Invalid(errors)
        }
        val normalized: ApiConfig = config.copy(
            name = config.name.trim(),
            url = config.url.trim(),
            headers = config.headers.map { it.copy(name = it.name.trim()) },
            createdAt = if (config.isNew) now else config.createdAt,
            updatedAt = now,
        )
        val id: Long = repository.save(normalized)
        AppLogger.i(TAG, "saved - {id: $id, name: ${normalized.name}, method: ${normalized.method}, url: ${normalized.url}}")

        return SaveApiConfigResult.Saved(id)
    }
}

class DuplicateApiConfigUseCase @Inject constructor(private val repository: ApiConfigRepository) {
    suspend operator fun invoke(config: ApiConfig, copySuffix: String): Long {
        val now: Long = TimeUtils.now()
        val id: Long = repository.save(
            config.copy(id = ApiConfig.NEW_ID, name = "${config.name} $copySuffix", createdAt = now, updatedAt = now)
        )
        AppLogger.i(TAG, "duplicated - {from: ${config.id}, to: $id}")

        return id
    }
}

class SetApiConfigEnabledUseCase @Inject constructor(private val repository: ApiConfigRepository) {
    suspend operator fun invoke(id: Long, enabled: Boolean) {
        repository.setEnabled(id, enabled)
        AppLogger.i(TAG, "enabled changed - {id: $id, enabled: $enabled}")
    }
}

class DeleteApiConfigUseCase @Inject constructor(private val repository: ApiConfigRepository) {
    suspend operator fun invoke(id: Long) {
        repository.delete(id)
        AppLogger.i(TAG, "deleted - {id: $id}")
    }
}
