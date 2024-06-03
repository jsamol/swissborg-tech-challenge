package com.example.swissborg_tech_challange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swissborg_tech_challange.data.Fiat
import com.example.swissborg_tech_challange.data.TradingPair
import com.example.swissborg_tech_challange.network.TradingPairsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
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
class MainViewModel @Inject constructor(private val api: TradingPairsApi) : ViewModel() {
    private val fiat: Fiat = Fiat.Usd

    private val _state: MutableStateFlow<MainState> = MutableStateFlow(MainState.Idle)
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                fetchTradingPairs()
                delay(5.seconds)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            fetchTradingPairs()
        }
    }

    private val fetchTradingPairsMutex: Mutex = Mutex()
    private var fetchTradingPairsJob: Job? = null
    private suspend fun fetchTradingPairs() = coroutineScope {
        val job = fetchTradingPairsMutex.withLock {
            fetchTradingPairsJob ?: launch {
                try {
                    val tradingPairs = api.tradingPairs(fiat)
                    _state.update {
                        it.copy(tradingPairs = tradingPairs)
                    }
                } catch (e: Throwable) {
                    // TODO: set error
                } finally {
                    fetchTradingPairsMutex.withLock { fetchTradingPairsJob = null }
                }
            }.also { fetchTradingPairsJob = it }
        }

        job.join()
    }
}

data class MainState(
    val tradingPairs: List<TradingPair>,
) {
    companion object {
        val Idle: MainState = MainState(
            tradingPairs = emptyList(),
        )
    }
}