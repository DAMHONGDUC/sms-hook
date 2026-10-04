package com.dd.sms.hook.features.apiconfig.data.repository

import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.apiconfig.data.local.ApiConfigDao
import com.dd.sms.hook.features.apiconfig.data.local.ApiConfigMapper
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ApiConfigRepositoryImpl @Inject constructor(
    private val dao: ApiConfigDao,
) : ApiConfigRepository {
    override fun observeAll(): Flow<List<ApiConfig>> =
        dao.observeAll().map { list -> list.map(ApiConfigMapper::toDomain) }

    override fun observeEnabledCount(): Flow<Int> = dao.observeEnabledCount()

    override suspend fun getById(id: Long): ApiConfig? = dao.getById(id)?.let(ApiConfigMapper::toDomain)

    override suspend fun getEnabled(): List<ApiConfig> = dao.getEnabled().map(ApiConfigMapper::toDomain)

    override suspend fun save(config: ApiConfig): Long {
        if (config.isNew) return dao.insert(ApiConfigMapper.toEntity(config))
        dao.update(ApiConfigMapper.toEntity(config))

        return config.id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled, TimeUtils.now())
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
