package com.example.swissborg_tech_challange.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class KtorHttpClientTest {

    @Test
    fun `should call HTTP GET`() = runTest {
        val url = "https://test.com"
        // language=json
        val responseJson = """
            {
              "text": "${UUID.randomUUID()}"
            }
        """.trimIndent()

        val engine = MockEngine {
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val ktor = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json)
            }
        }
        val httpClient = KtorHttpClient(ktor)

        val response = httpClient.get(url)

        assertEquals(Json.parseToJsonElement(responseJson), response)
        assertEquals(1, engine.requestHistory.size)
        assertEquals(url, engine.requestHistory.first().url.toString())
        assertEquals(true, engine.requestHistory.first().headers.getAll(HttpHeaders.ContentType)?.contains("application/json"))
        assertEquals(true, engine.requestHistory.first().headers.getAll(HttpHeaders.Accept)?.contains("application/json"))
        assertEquals(0L, engine.requestHistory.first().body.contentLength)
    }

    @Test
    fun `should call HTTP GET with additional headers`() = runTest {
        val url = "https://test.com"
        val headers = listOf(
            "Authorization" to "Bearer ${UUID.randomUUID()}",
        )

        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val ktor = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json)
            }
        }
        val httpClient = KtorHttpClient(ktor)

        httpClient.get(url, headers = headers)

        assertEquals(1, engine.requestHistory.size)
        assertEquals(url, engine.requestHistory.first().url.toString())
        assertEquals(true, engine.requestHistory.first().headers.getAll(HttpHeaders.ContentType)?.contains("application/json"))
        assertEquals(true, engine.requestHistory.first().headers.getAll(HttpHeaders.Accept)?.contains("application/json"))
        headers.forEach { (key, value) ->
            assertEquals(true, engine.requestHistory.first().headers.getAll(key)?.contains(value))
        }
        assertEquals(0L, engine.requestHistory.first().body.contentLength)
    }

    @Test
    fun `should call HTTP GET with additional parameters`() = runTest {
        val url = "https://test.com"
        val parameters = listOf(
            "ids" to (0..<5).joinToString(",") { "$it:${UUID.randomUUID()}" },
        )

        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val ktor = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json)
            }
        }
        val httpClient = KtorHttpClient(ktor)

        httpClient.get(url, parameters = parameters)

        assertEquals(1, engine.requestHistory.size)
        assertEquals("$url?${parameters.joinToString("&") { "${it.first}=${it.second}" }}", engine.requestHistory.first().url.toString())
        assertEquals(true, engine.requestHistory.first().headers.getAll(HttpHeaders.ContentType)?.contains("application/json"))
        assertEquals(true, engine.requestHistory.first().headers.getAll(HttpHeaders.Accept)?.contains("application/json"))
        assertEquals(0L, engine.requestHistory.first().body.contentLength)
    }
}