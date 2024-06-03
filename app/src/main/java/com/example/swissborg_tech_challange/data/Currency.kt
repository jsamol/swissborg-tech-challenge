package com.example.swissborg_tech_challange.data

data class Crypto(val name: String, val symbol: String)

enum class Fiat(val code: String, val symbol: String) {
    Usd(code = "USD", symbol = "$"),
}