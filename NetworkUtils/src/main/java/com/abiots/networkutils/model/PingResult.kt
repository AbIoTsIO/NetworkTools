package com.abiots.networkutils.model

import java.net.InetAddress

data class PingResult(
    val inetAddress: InetAddress,
    var isReachable: Boolean = false,
    var error: String? = null,
    var timeTaken: Float = 0f,
    var result: String? = null,
) {
    fun toDevice() = run {
      return@run  Device(ip = inetAddress.hostAddress!!, hostName = inetAddress.hostName, macAddress = "123")
    }
}
