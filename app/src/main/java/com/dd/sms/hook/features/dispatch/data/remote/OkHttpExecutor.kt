package com.dd.sms.hook.features.dispatch.data.remote

import com.dd.sms.hook.shared.domain.constants.HttpConstants
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.dispatch.domain.model.HttpRequestSpec
import com.dd.sms.hook.features.dispatch.domain.model.HttpResult
import com.dd.sms.hook.features.dispatch.domain.service.HttpExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "OkHttpExecutor"

class OkHttpExecutor @Inject constructor(
    private val client: OkHttpClient,
) : HttpExecutor {
    override suspend fun execute(request: HttpRequestSpec, timeoutSeconds: Int): HttpResult {
        val startedAt: Long = System.nanoTime()

        return try {
            val okRequest: Request = toOkRequest(request)
            val call = client.newBuilder()
                .callTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .build()
                .newCall(okRequest)

            runInterruptible(Dispatchers.IO) {
                call.execute().use { response: Response ->
                    val body: String = response.peekBody(HttpConstants.MAX_STORED_BODY_BYTES.toLong()).string()
                    HttpResult.Response(code = response.code, body = body, durationMs = elapsedMs(startedAt))
                }
            }
        } catch (e: Exception) {
            // Transport errors, timeouts and malformed URLs/headers all become a logged failure result.
            AppLogger.e(TAG, "request failed - {method: ${request.method}, url: ${request.url}}", e)
            HttpResult.Failure(message = "${e::class.simpleName}: ${e.message}", durationMs = elapsedMs(startedAt))
        }
    }

    private fun toOkRequest(request: HttpRequestSpec): Request {
        val headers: Headers = Headers.Builder().apply {
            request.headers.forEach { add(it.name, it.value) }
        }.build()
        val mediaType: MediaType? = headers[HttpConstants.CONTENT_TYPE_HEADER]?.toMediaTypeOrNull()
        val body: RequestBody? = request.body?.toRequestBody(mediaType)

        return Request.Builder()
            .url(request.url)
            .headers(headers)
            .method(request.method.name, body)
            .build()
    }

    private fun elapsedMs(startedAt: Long): Long = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
}
