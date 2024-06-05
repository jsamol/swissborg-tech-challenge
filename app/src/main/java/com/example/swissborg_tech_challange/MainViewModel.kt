package com.example.swissborg_tech_challange

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swissborg_tech_challange.data.Fiat
import com.example.swissborg_tech_challange.data.TradingPair
import com.example.swissborg_tech_challange.network.NetworkState
import com.example.swissborg_tech_challange.network.TradingPairsApi
import com.example.swissborg_tech_challange.ui.screen.DashboardState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class MainViewModel @Inject constructor(
    private val api: TradingPairsApi,
    private val networkState: NetworkState,
) : ViewModel() {
    private val fiat: Fiat = Fiat.Usd

    private val _state: MutableStateFlow<MainState> = MutableStateFlow(MainState.Idle)
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val fetchTradingPairsMutex: Mutex = Mutex()
    private var fetchTradingPairsDeferred: Job? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                fetchTradingPairs()
                delay(5.seconds)
            }
        }

        viewModelScope.launch {
            networkState.isOnline.collect {
                _state.update { state ->
                    state.copy(isOnline = it)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    dashboard = state.dashboard.copy(
                        isRefreshing = true,
                    ),
                )
            }

            fetchTradingPairs()

            _state.update { state ->
                state.copy(
                    dashboard = state.dashboard.copy(
                        isRefreshing = false
                    )
                )
            }
        }
    }

    fun filter(query: String?) {
        viewModelScope.launch {
            val query = query?.takeIf { it.isNotBlank() }
            _state.update { state ->
                state.copy(
                    dashboard = state.dashboard.copy(
                        tradingPairs = state.tradingPairs.filterWith(query),
                        filter = query,
                    ),
                )
            }
        }
    }

    private suspend fun fetchTradingPairs() = coroutineScope {
        val job = fetchTradingPairsMutex.withLock {
            fetchTradingPairsDeferred ?: launch {
                try {
                    runCatching { api.tradingPairs(fiat) }
                        .onSuccess { tradingPairs ->
                            _state.update { state ->
                                state.copy(
                                    tradingPairs = tradingPairs,
                                    failures = 0,
                                    dashboard = state.dashboard.copy(
                                        tradingPairs = tradingPairs.filterWith(state.dashboard.filter),
                                    ),
                                )
                            }
                        }
                        .onFailure {
                            Log.d("MainViewModel", "fetchTradingPairs failed", it)
                            _state.update { state ->
                                state.copy(failures = state.failures + 1)
                            }
                        }
                } finally {
                    fetchTradingPairsMutex.withLock { fetchTradingPairsDeferred = null }
                }
            }.also { fetchTradingPairsDeferred = it }
        }

        job.join()
    }

    private fun List<TradingPair>.filterWith(query: String?): List<TradingPair> =
        if (query != null) filter { it.matches(query) } else this

    private fun TradingPair.matches(query: String): Boolean =
        crypto.name.lowercase().contains(query.lowercase()) || crypto.symbol.lowercase().contains(query.lowercase())
}

data class MainState(
    val tradingPairs: List<TradingPair>,
    val isOnline: Boolean,
    val failures: Int,
    val dashboard: DashboardState,
) {
    companion object {
        val Idle: MainState = MainState(
            tradingPairs = emptyList(),
            isOnline = true,
            failures = 0,
            dashboard = DashboardState.Idle,
        )
    }
}