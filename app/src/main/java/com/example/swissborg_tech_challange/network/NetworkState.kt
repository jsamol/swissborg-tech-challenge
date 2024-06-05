package com.example.swissborg_tech_challange.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class NetworkState @Inject constructor(@ApplicationContext private val context: Context) {
    private val connectivityManager: ConnectivityManager?
        get() = context.getSystemService<ConnectivityManager>()

    private val networkRequest: NetworkRequest = NetworkRequest.Builder().apply {
        addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
    }.build()

    val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = connectivityManager ?: run {
            send(false)
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
                super.onAvailable(network)
            }

            override fun onLost(network: Network) {
                trySend(false)
                super.onLost(network)
            }
        }

        connectivityManager.requestNetwork(networkRequest, callback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}