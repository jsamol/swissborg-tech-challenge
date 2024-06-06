@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.swissborg_tech_challange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.swissborg_tech_challange.data.Crypto
import com.example.swissborg_tech_challange.data.Fiat
import com.example.swissborg_tech_challange.data.TradingPair
import com.example.swissborg_tech_challange.ui.screen.Dashboard
import com.example.swissborg_tech_challange.ui.screen.DashboardState
import com.example.swissborg_tech_challange.ui.theme.SwissborgtechchallangeTheme
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.pollTradingPairs()
        viewModel.observeNetworkState()

        setContent {
            SwissborgtechchallangeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsState()
                    Main(
                        state = state,
                        refresh = { viewModel.refresh() },
                        filter = { viewModel.filter(it) },
                    )
                }
            }
        }
    }
}

@Composable
fun Main(
    state: MainState,
    refresh: () -> Unit = {},
    filter: (String?) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Tech Challenge")
                    }
                )
            }
        ) { padding ->
            Dashboard(
                modifier = Modifier.padding(padding),
                state = state.dashboard,
                refresh = refresh,
                filter = filter,
            )
        }

        val error =
            if (!state.isOnline) "Your device appears to be offline"
            else if (state.failures >= 3) "We're having some troubles, try again later"
            else null

        AnimatedVisibility(
            visible = error != null,
            enter = slideInVertically() + expandVertically() + fadeIn(),
            exit = slideOutVertically() + shrinkVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainIdlePreview() {
    val tradingPairs = listOf(
        TradingPair(
            crypto = Crypto("Bitcoin", "BTC"),
            fiat = Fiat.Usd,
            price = BigDecimal.valueOf(71_396),
            change = BigDecimal.valueOf(0.757843),
        ),
        TradingPair(
            crypto = Crypto("Ethereum", "ETH"),
            fiat = Fiat.Usd,
            price = BigDecimal.valueOf(3_805),
            change = BigDecimal.valueOf(-0.626796),
        ),
        TradingPair(
            crypto = Crypto("Litecoin", "LTC"),
            fiat = Fiat.Usd,
            price = BigDecimal.valueOf(84.658),
            change = BigDecimal.valueOf(2.0098816),
        ),
    )

    SwissborgtechchallangeTheme {
        Main(
            state = MainState(
                tradingPairs = tradingPairs,
                isOnline = true,
                failures = 0,
                dashboard = DashboardState(
                    tradingPairs = tradingPairs,
                    filter = null,
                    isRefreshing = false,
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    SwissborgtechchallangeTheme {
        Main(
            state = MainState.Idle,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainOfflinePreview() {
    SwissborgtechchallangeTheme {
        Main(
            state = MainState.Idle.copy(isOnline = false),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainFailurePreview() {
    SwissborgtechchallangeTheme {
        Main(
            state = MainState.Idle.copy(failures = 3),
        )
    }
}