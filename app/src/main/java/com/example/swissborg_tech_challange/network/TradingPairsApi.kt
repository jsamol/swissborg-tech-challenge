package com.example.swissborg_tech_challange.network

import com.example.swissborg_tech_challange.data.Crypto
import com.example.swissborg_tech_challange.data.Fiat
import com.example.swissborg_tech_challange.data.TradingPair
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import javax.inject.Inject

interface TradingPairsApi {
    suspend fun tradingPairs(fiat: Fiat): List<TradingPair>
}

class TradingPairsBitfinex @Inject constructor(private val httpClient: HttpClient) : TradingPairsApi {

    override suspend fun tradingPairs(fiat: Fiat): List<TradingPair> = coroutineScope {
        val tradingPairsDeferred = async { pairExchangeList(fiat) }
        val cryptoLabelsDeferred = async { currencyLabelMap() }

        val tradingPairs = tradingPairsDeferred.await().takeIf { it.isNotEmpty() } ?: return@coroutineScope emptyList()
        val cryptoLabels = cryptoLabelsDeferred.await()

        val tickers = tickers(symbols = tradingPairs)
            .mapNotNull { obj ->
                val symbol = obj.getOrNull(0)?.takeIf { it.jsonPrimitive.isString }?.toString() ?: return@mapNotNull null
                val change = obj.getOrNull(6)?.jsonPrimitive?.floatOrNull ?: return@mapNotNull null
                val price = obj.getOrNull(7)?.jsonPrimitive?.floatOrNull ?: return@mapNotNull null

                val crypto = cryptoFromSymbol(symbol, fiat, cryptoLabels)

                TradingPair(
                    crypto,
                    fiat,
                    BigDecimal.valueOf(change.toDouble()),
                    BigDecimal.valueOf(price.toDouble()),
                )
            }

        tickers
    }

    private suspend fun pairExchangeList(fiat: Fiat): List<String> =
        httpClient.get<List<List<String>>>(endpoint("/conf/pub:list:pair:exchange"))
            .firstOrNull()
            ?.filter { it.lowercase().endsWith(fiat.code.lowercase()) }
            ?: emptyList()

    private suspend fun currencyLabelMap(): Map<String, String> =
        httpClient.get<List<List<Pair<String, String>>>>(endpoint("/conf/pub:map:currency:label"))
            .firstOrNull()
            ?.toMap()
            ?: emptyMap()

    private suspend fun tickers(symbols: List<String>): List<List<JsonElement>> =
        httpClient.get(endpoint("/tickers"), parameters = listOf("symbols" to symbols.joinToString(",")))

    private fun endpoint(name: String): String = "$BASE_URL/${name.trimStart('/')}"

    private fun cryptoFromSymbol(
        symbol: String,
        fiat: Fiat,
        names: Map<String, String>,
    ): Crypto {
        val crypto = symbol.removeSurrounding(prefix = "t", suffix = fiat.code).removeSuffix(":")

        return Crypto(name = names[crypto] ?: crypto, crypto)
    }

    companion object {
        private const val BASE_URL: String = "https://api-pub.bitfinex.com/v2"
    }
}

@Module
@InstallIn(ActivityComponent::class)
abstract class TradingPairsApiModule {

    @Binds
    abstract fun bindTradingPairsApi(bitfinex: TradingPairsBitfinex): TradingPairsApi
}