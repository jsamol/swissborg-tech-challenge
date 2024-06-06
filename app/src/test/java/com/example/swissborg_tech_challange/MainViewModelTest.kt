package com.example.swissborg_tech_challange

import com.example.swissborg_tech_challange.data.Crypto
import com.example.swissborg_tech_challange.data.Fiat
import com.example.swissborg_tech_challange.data.TradingPair
import com.example.swissborg_tech_challange.network.ApiClient
import com.example.swissborg_tech_challange.network.NetworkState
import com.example.swissborg_tech_challange.util.debug
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MainViewModelTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var apiClient: ApiClient

    @MockK
    private lateinit var networkState: NetworkState

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        mockkStatic(::debug)
        every { debug(any(), any(), any()) } answers { println("DEBUG: [${firstArg<String>()}]: ${secondArg<String>()}") }

        viewModel = MainViewModel(apiClient, networkState)
    }

    @Test
    fun `should fetch trading pairs every 5 seconds`() = runTestBlocking {
        val initState = viewModel.state.value

        val tradingPairs = listOf(
            TradingPair(
                Crypto(name = "Bitcoin", symbol = "BTC"),
                Fiat.Usd,
                price = BigDecimal.valueOf(70953),
                change = BigDecimal.valueOf(-0.175863),
            ),
            TradingPair(
                Crypto(name = "Ethereum", symbol = "ETH"),
                Fiat.Usd,
                price = BigDecimal.valueOf(3848.1),
                change = BigDecimal.valueOf(1.042433),
            ),
        )

        coEvery { apiClient.tradingPairs(Fiat.Usd) } returns tradingPairs

        viewModel.pollTradingPairs()
        delay(11.seconds)

        assertEquals(
            initState.copy(
                tradingPairs = tradingPairs,
                failures = 0,
                dashboard = initState.dashboard.copy(
                    tradingPairs = tradingPairs,
                    filter = null,
                    isRefreshing = false,
                ),
            ),
            viewModel.state.value,
        )

        coVerify(exactly = 3) { apiClient.tradingPairs(any()) }
    }

    @Test
    fun `should respond to connectivity changes`() = runTestBlocking {
        val initState = viewModel.state.value

        every { networkState.isOnline } returns flow {
            emit(true)
            delay(750.milliseconds)
            emit(false)
        }
        viewModel.observeNetworkState()

        assertEquals(
            initState.copy(isOnline = true),
            viewModel.state.value,
        )

        delay(1.seconds)

        assertEquals(
            initState.copy(isOnline = false),
            viewModel.state.value,
        )

    }

    @Test
    fun `should fetch trading pairs on refresh`() = runTestBlocking {
        val initState = viewModel.state.value

        val tradingPairs = listOf(
            TradingPair(
                Crypto(name = "Bitcoin", symbol = "BTC"),
                Fiat.Usd,
                price = BigDecimal.valueOf(70953),
                change = BigDecimal.valueOf(-0.175863),
            ),
            TradingPair(
                Crypto(name = "Ethereum", symbol = "ETH"),
                Fiat.Usd,
                price = BigDecimal.valueOf(3848.1),
                change = BigDecimal.valueOf(1.042433),
            ),
        )

        coEvery { apiClient.tradingPairs(Fiat.Usd) } coAnswers {
            delay(750.milliseconds)
            tradingPairs
        }

        viewModel.refresh()

        assertEquals(
            initState.copy(
                dashboard = initState.dashboard.copy(
                    isRefreshing = true,
                ),
            ),
            viewModel.state.value,
        )

        delay(1.seconds)

        assertEquals(
            initState.copy(
                tradingPairs = tradingPairs,
                failures = 0,
                dashboard = initState.dashboard.copy(
                    tradingPairs = tradingPairs,
                    filter = null,
                    isRefreshing = false,
                ),
            ),
            viewModel.state.value,
        )

        coVerify(exactly = 1) { apiClient.tradingPairs(any()) }
    }

    @Test
    fun `should filter trading pairs`() = runTest {
        val tradingPairs = listOf(
            TradingPair(
                Crypto(name = "Bitcoin", symbol = "BTC"),
                Fiat.Usd,
                price = BigDecimal.valueOf(70953),
                change = BigDecimal.valueOf(-0.175863),
            ),
            TradingPair(
                Crypto(name = "Ethereum", symbol = "ETH"),
                Fiat.Usd,
                price = BigDecimal.valueOf(3848.1),
                change = BigDecimal.valueOf(1.042433),
            ),
        )

        coEvery { apiClient.tradingPairs(Fiat.Usd) } returns tradingPairs

        viewModel.refresh()

        val initState = viewModel.state.value

        val query = "btc"
        viewModel.filter(query)

        assertEquals(
            initState.copy(
                dashboard = initState.dashboard.copy(
                    tradingPairs = listOf(
                        TradingPair(
                            Crypto(name = "Bitcoin", symbol = "BTC"),
                            Fiat.Usd,
                            price = BigDecimal.valueOf(70953),
                            change = BigDecimal.valueOf(-0.175863),
                        ),
                    ),
                    filter = query
                ),
            ),
            viewModel.state.value,
        )

        coVerify(exactly = 1) { apiClient.tradingPairs(any()) }
    }

    @Test
    fun `should track failures`() = runTestBlocking {
        val initState = viewModel.state.value

        coEvery { apiClient.tradingPairs(Fiat.Usd) } throws RuntimeException("Error")

        viewModel.refresh()
        viewModel.refresh()
        viewModel.refresh()

        delay(500.milliseconds)

        assertEquals(
            initState.copy(failures = 3),
            viewModel.state.value,
        )

        coVerify(exactly = 3) { apiClient.tradingPairs(any()) }
    }

    @Test
    fun `should reset failures upon first success`() = runTestBlocking {
        val initState = viewModel.state.value

        coEvery { apiClient.tradingPairs(Fiat.Usd) } throws RuntimeException("Error")

        viewModel.refresh()
        viewModel.refresh()
        viewModel.refresh()

        delay(500.milliseconds)

        assertEquals(
            initState.copy(failures = 3),
            viewModel.state.value,
        )

        coEvery { apiClient.tradingPairs(Fiat.Usd) } returns emptyList()

        viewModel.refresh()

        delay(500.milliseconds)

        assertEquals(
            initState.copy(failures = 0),
            viewModel.state.value,
        )

        coVerify(exactly = 4) { apiClient.tradingPairs(any()) }
    }

    @Test
    fun `should call API only once at the time`() = runTestBlocking {
        coEvery { apiClient.tradingPairs(Fiat.Usd) } coAnswers {
            delay(1.seconds)
            emptyList()
        }

        viewModel.refresh()
        viewModel.refresh()
        viewModel.refresh()

        delay(1.seconds)

        coVerify(exactly = 1) { apiClient.tradingPairs(any()) }
    }

    private fun runTestBlocking(dispatcher: CoroutineDispatcher = Dispatchers.Unconfined, block: suspend CoroutineScope.() -> Unit) = runBlocking {
        Dispatchers.setMain(dispatcher)
        block()
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
        override fun starting(description: Description?) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description?) {
            Dispatchers.resetMain()
        }
    }
}