package com.example.swissborg_tech_challange.network

import com.example.swissborg_tech_challange.data.Crypto
import com.example.swissborg_tech_challange.data.Fiat
import com.example.swissborg_tech_challange.data.TradingPair
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class BitfinexApiClientTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    lateinit var httpClientProvider: HttpClientProvider

    lateinit var apiClient: BitfinexApiClient

    @Before
    fun setup() {
        apiClient = BitfinexApiClient(HttpClient(httpClientProvider, Json))
    }

    @Test
    fun `should fetch trading pair list (USD)`() = runTest {
        val fiat = Fiat.Usd
        val pairExchangeList = listOf("BTCUSD", "ETHUSD", "LTCUSD")
        val currencyLabelMap = mapOf(
            "BTC" to "Bitcoin",
            "ETH" to "Ethereum",
            "LTC" to "Litecoin",
        )
        // language=json
        val tickers = Json.parseToJsonElement("""
            [
                [
                    "tBTCUSD",
                    70952,
                    6.94583373,
                    70953,
                    5.03094031,
                    -125,
                    -0.00175863,
                    70953,
                    813.01523732,
                    71717,
                    70409
                ],
                [
                    "tETHUSD",
                    3849.5,
                    129.78610931,
                    3849.9,
                    144.58197293,
                    39.7,
                    0.01042433,
                    3848.1,
                    3490.5635839,
                    3884,
                    3782.2
                ],
                [
                    "tLTCUSD",
                    85.286,
                    826.13449577,
                    85.319,
                    1060.62666752,
                    0.993,
                    0.01179279,
                    85.197,
                    7424.33053588,
                    85.45,
                    83.633
                ]
            ]
        """.trimIndent())

        coEvery {
            httpClientProvider.get(
                match { it.endsWith("/pub:list:pair:exchange") },
                any(),
                any(),
            )
        } returns Json.encodeToJsonElement(listOf(pairExchangeList))

        coEvery {
            httpClientProvider.get(
                match { it.endsWith("/pub:map:currency:label") },
                any(),
                any(),
            )
        } returns Json.encodeToJsonElement(listOf(currencyLabelMap.map { listOf(it.key, it.value) }))

        coEvery {
            httpClientProvider.get(
                match { it.endsWith("/tickers") },
                any(),
                parameters = listOf(
                    "symbols" to pairExchangeList.joinToString(",") { "t$it" },
                ),
            )
        } returns Json.decodeFromJsonElement(tickers)

        val tradingPairs = apiClient.tradingPairs(fiat)

        assertEquals(
            listOf(
                TradingPair(
                    Crypto(name = "Bitcoin", symbol = "BTC"),
                    fiat,
                    price = BigDecimal.valueOf(70953),
                    change = BigDecimal.valueOf(-0.175863),
                ),
                TradingPair(
                    Crypto(name = "Ethereum", symbol = "ETH"),
                    fiat,
                    price = BigDecimal.valueOf(3848.1),
                    change = BigDecimal.valueOf(1.042433),
                ),
                TradingPair(
                    Crypto(name = "Litecoin", symbol = "LTC"),
                    fiat,
                    price = BigDecimal.valueOf(85.197),
                    change = BigDecimal.valueOf(1.179279),
                ),
            ),
            tradingPairs,
        )
    }
}