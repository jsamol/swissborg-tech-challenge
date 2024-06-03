package com.example.swissborg_tech_challange.data

import java.math.BigDecimal

data class TradingPair(
    val crypto: Crypto,
    val fiat: Fiat,
    val price: BigDecimal,
    val change: BigDecimal,
)
