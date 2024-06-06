@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.swissborg_tech_challange.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.swissborg_tech_challange.data.Crypto
import com.example.swissborg_tech_challange.data.Fiat
import com.example.swissborg_tech_challange.data.TradingPair
import com.example.swissborg_tech_challange.ui.theme.PaddingContentHorizontal
import com.example.swissborg_tech_challange.ui.theme.PaddingContentVertical
import com.example.swissborg_tech_challange.ui.theme.SpaceM
import com.example.swissborg_tech_challange.ui.theme.SpaceS
import com.example.swissborg_tech_challange.ui.theme.SpaceXS
import com.example.swissborg_tech_challange.ui.theme.SwissborgtechchallangeTheme
import com.example.swissborg_tech_challange.util.round
import com.example.swissborg_tech_challange.util.toString
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class DashboardState(
    val tradingPairs: List<TradingPair>,
    val filter: String?,
    val isRefreshing: Boolean,
) {
    companion object {
        val Idle: DashboardState = DashboardState(
            tradingPairs = emptyList(),
            filter = null,
            isRefreshing = false,
        )
    }
}

@Composable
fun Dashboard(
    modifier: Modifier = Modifier,
    state: DashboardState,
    refresh: () -> Unit = {},
    filter: (String?) -> Unit = {},
) {
    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            refresh()
        }
    }
    if (!state.isRefreshing) {
        LaunchedEffect(true) {
            pullToRefreshState.endRefresh()
        }
    }

    Box(
        Modifier
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
            .then(modifier)
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PaddingContentHorizontal)
        ) {
            SearchBar(
                query = state.filter ?: "",
                onQueryChange = { filter(it) },
                onSearch = { filter(it) },
                active = false,
                onActiveChange = {},
                placeholder = {
                    Text(text = "BTC, ETH, etc.")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                    )
                },
                trailingIcon = {
                    if (state.filter?.isNotEmpty() == true) {
                        IconButton(onClick = { filter(null) }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear icon",
                            )
                        }
                    }
                },
                content = {},
            )
            if (state.tradingPairs.isNotEmpty()) TradingPairList(tradingPairs = state.tradingPairs)
            else EmptyTradingPairList()
        }
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun EmptyTradingPairList() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Oops! List is empty",
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun TradingPairList(
    modifier: Modifier = Modifier,
    tradingPairs: List<TradingPair>,
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(SpaceM),
            contentPadding = PaddingValues(vertical = PaddingContentVertical),
            modifier = Modifier
                .then(modifier)
                .fillMaxSize(),
        ) {
            items(tradingPairs, key = { it.crypto.symbol }) {
                TradingPairRow(tradingPair = it)
            }
        }

        if (lazyListState.canScrollBackward) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(SpaceM),
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(index = 0)
                        }
                    },
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
        }
    }
}

@Composable
private fun TradingPairRow(modifier: Modifier = Modifier, tradingPair: TradingPair) {
    ElevatedCard(modifier = Modifier
        .then(modifier)
        .fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle, // placeholder icon
                contentDescription = "${tradingPair.crypto.symbol} icon",
            )
            Spacer(modifier = Modifier.width(SpaceM))
            TradingPairLabel(modifier = Modifier.weight(1f), tradingPair = tradingPair)
            Spacer(modifier = Modifier.width(SpaceM))
            TradingPairPrice(tradingPair = tradingPair)
        }
    }
}

@Composable
private fun TradingPairLabel(modifier: Modifier = Modifier, tradingPair: TradingPair) {
    Column(
        modifier = Modifier.then(modifier),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = tradingPair.crypto.name,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(SpaceXS))
        Text(
            text = tradingPair.crypto.symbol,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun TradingPairPrice(modifier: Modifier = Modifier, tradingPair: TradingPair) {
    Column(
        modifier = Modifier.then(modifier),
        horizontalAlignment = Alignment.End
    ) {
        val price = tradingPair.price.round()
        val change = tradingPair.change.round()

        Text(
            text = "${tradingPair.fiat.symbol} ${price.toString(withSign = false)}",
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(SpaceXS))
        Text(
            text = "${change.toString(withSign = true)}%",
            color = (
                    if (change > BigDecimal.ZERO) Color.Green
                    else if (change == BigDecimal.ZERO) Color.Gray
                    else Color.Red
                    ).copy(alpha = 0.5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardIdlePreview() {
    SwissborgtechchallangeTheme {
        Dashboard(
            state = DashboardState.Idle,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    SwissborgtechchallangeTheme {
        Dashboard(
            state = DashboardState(
                tradingPairs = listOf(
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
                ),
                filter = null,
                isRefreshing = false,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardFilterPreview() {
    SwissborgtechchallangeTheme {
        Dashboard(
            state = DashboardState(
                tradingPairs = listOf(
                    TradingPair(
                        crypto = Crypto("Ethereum", "ETH"),
                        fiat = Fiat.Usd,
                        price = BigDecimal.valueOf(3_805),
                        change = BigDecimal.valueOf(-0.626796),
                    ),
                ),
                filter = "eth",
                isRefreshing = false,
            )
        )
    }
}