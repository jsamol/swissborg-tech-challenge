package com.example.swissborg_tech_challange.network

import com.example.swissborg_tech_challange.data.Crypto
import com.example.swissborg_tech_challange.data.Fiat
import com.example.swissborg_tech_challange.data.TradingPair
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import javax.inject.Inject

interface ApiClient {
    suspend fun tradingPairs(fiat: Fiat): List<TradingPair>
}

class BitfinexApiClient @Inject constructor(private val http: HttpClient) : ApiClient {

    override suspend fun tradingPairs(fiat: Fiat): List<TradingPair> = coroutineScope {
        val tradingPairs = async { pairExchangeList(fiat) }
        val cryptoLabels = async { currencyLabelMap() }

        val tickers = tickers(symbols = tradingPairs.await())
            .mapNotNull { obj ->
                val symbol = obj.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val change = obj.getOrNull(6)?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val price = obj.getOrNull(7)?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

                val crypto = cryptoFromSymbol(symbol, fiat, cryptoLabels.await())

                TradingPair(
                    crypto,
                    fiat,
                    BigDecimal(price),
                    (BigDecimal(change) * BigDecimal.valueOf(100 /* % */)).stripTrailingZeros(),
                )
            }

        tickers
    }

    private suspend fun pairExchangeList(fiat: Fiat): List<String> =
        http.get<List<List<String>>>(endpoint("/conf/pub:list:pair:exchange"))
            .firstOrNull()
            ?.filter { it.lowercase().endsWith(fiat.code.lowercase()) }
            ?: emptyList()

    private suspend fun currencyLabelMap(): Map<String, String> =
        http.get<List<List<List<String>>>>(endpoint("/conf/pub:map:currency:label"))
            .firstOrNull()
            ?.filter { it.size == 2 }
            ?.associate { it[0] to it[1] }
            ?: emptyMap()

    private suspend fun tickers(symbols: List<String>): List<List<JsonElement>> =
        http.get(endpoint("/tickers"), parameters = listOf("symbols" to symbols.joinToString(",") { "t$it" }))

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
@InstallIn(ViewModelComponent::class)
abstract class ApiClientBindingModule {

    @Binds
    @Reusable
    abstract fun bindApiClient(bitfinex: BitfinexApiClient): ApiClient
}