package com.example.swissborg_tech_challange.network

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpClientTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    lateinit var provider: HttpClientProvider

    lateinit var httpClient: HttpClient

    @Before
    fun setup() {
        httpClient = HttpClient(provider, Json)
    }

    @Test
    fun `should use provider to call HTTP GET and decode response`() = runTest {
        val url = "https://test.com"
        val text = UUID.randomUUID().toString()

        coEvery { provider.get(any(), any(), any()) } returns Json.parseToJsonElement(
            // language=json
            """
                {
                  "text": "$text"
                }
            """.trimIndent()
        )

        val response = httpClient.get<MockResponse>(url)

        assertEquals(text, response.text)
        coVerify { provider.get(url, emptyList(), emptyList()) }
    }

    @Test
    fun `should use provider to call HTTP GET with additional headers`() = runTest {
        coEvery { provider.get(any(), any(), any()) } returns JsonNull

        val url = "https://test.com"
        val headers = listOf(
            "Authorization" to "Bearer ${UUID.randomUUID()}",
        )
        httpClient.get<Unit>(url, headers = headers)

        coVerify { provider.get(url, headers, emptyList()) }
    }

    @Test
    fun `should use provider to call HTTP GET with additional parameters`() = runTest {
        coEvery { provider.get(any(), any(), any()) } returns JsonNull

        val url = "https://test.com"
        val parameters = listOf(
            "id" to UUID.randomUUID().toString(),
        )
        httpClient.get<Unit>(url, parameters = parameters)

        coVerify { provider.get(url, emptyList(), parameters) }
    }

    @Test
    fun `should fail on provider failing`() = runTest {
        val exception = RuntimeException("Error")
        coEvery { provider.get(any(), any(), any()) } throws exception

        val url = "https://test.com"
        assertFailsWith<RuntimeException>(exception.message) {
            httpClient.get<Unit>(url)
        }
    }

    @Serializable
    data class MockResponse(val text: String)
}