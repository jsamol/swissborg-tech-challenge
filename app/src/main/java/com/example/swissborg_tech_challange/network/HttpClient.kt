package com.example.swissborg_tech_challange.network

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import javax.inject.Inject

typealias HttpHeader = Pair<String, String?>
typealias HttpParameter = Pair<String, String?>

class HttpClient @Inject constructor(
    @PublishedApi internal val provider: HttpClientProvider,
    @PublishedApi internal val json: Json,
) {
    suspend inline fun <reified Response : Any> get(
        url: String,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
    ): Response = provider.get(url, headers, parameters).decodeAsResponse()

    @PublishedApi
    internal inline fun <reified T : Any> JsonElement.decodeAsResponse(): T =
        when (T::class) {
            Unit::class -> Unit as T
            else -> json.decodeFromJsonElement(serializer(), this)
        }
}

interface HttpClientProvider {
    suspend fun get(
        url: String,
        headers: List<HttpHeader> = emptyList(),
        parameters: List<HttpParameter> = emptyList(),
    ): JsonElement
}

class KtorHttpClient @Inject constructor(private val ktor: io.ktor.client.HttpClient) : HttpClientProvider {
    override suspend fun get(
        url: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
    ): JsonElement = request(HttpMethod.Get, url, headers, parameters)

    private suspend fun request(
        method: HttpMethod,
        url: String,
        headers: List<HttpHeader>,
        parameters: List<HttpParameter>,
        block: HttpRequestBuilder.() -> Unit = {},
    ): JsonElement = ktor.request {
        this.method = method
        this.url(url)

        headers.forEach { header(it.first, it.second) }
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)

        parameters.forEach { parameter(it.first, it.second) }

        block(this)
    }.body()
}

@Module
@InstallIn(ActivityComponent::class)
abstract class HttpModule {

    @Binds
    abstract fun bindHttpClientProvider(provider: KtorHttpClient): HttpClientProvider

    @Provides
    fun provideKtor(json: Json): io.ktor.client.HttpClient = io.ktor.client.HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
    }

    @Provides
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
}