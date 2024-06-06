package com.example.swissborg_tech_challange.util

import android.util.Log

fun debug(tag: String, message: String, throwable: Throwable? = null) {
    Log.d(tag, message, throwable)
}