package com.nikashitsa.polar_alert_android.lib

import androidx.compose.runtime.saveable.Saver

sealed class DeviceConnectionState {
    abstract val address: String
    data class Disconnected(override val address: String = ""): DeviceConnectionState()
    data class Connecting(override val address: String = ""): DeviceConnectionState()
    data class Connected(override val address: String = ""): DeviceConnectionState()

    companion object {
        val Saver: Saver<DeviceConnectionState, Pair<String, String>> = Saver(
            save = {
                val type = when (it) {
                    is Disconnected -> "Disconnected"
                    is Connecting -> "Connecting"
                    is Connected -> "Connected"
                }
                type to it.address
            },
            restore = {
                val (type, address) = it
                when (type) {
                    "Disconnected" -> Disconnected(address)
                    "Connecting" -> Connecting(address)
                    "Connected" -> Connected(address)
                    else -> Disconnected()
                }
            }
        )
    }
}