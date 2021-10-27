package com.abiots.networkutils.utils

import com.abiots.networkutils.BuildConfig
import timber.log.Timber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * @author J Suhas Bhat
 */
object NetworkUtils {

    init {
        if (Timber.treeCount() == 0 && BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else Unit
    }


    fun isIPv4Address(ipAddress: String): Boolean = runCatching {
        (InetAddress.getByName(ipAddress) is Inet4Address)
    }.getOrDefault(false)

    fun isIPv6Address(ipAddress: String): Boolean = runCatching {
        (InetAddress.getByName(ipAddress) is Inet6Address)
    }.getOrDefault(false)

    fun isIpFromLocalNetwork(ipAddress: String): Boolean = runCatching {
        InetAddress.getByName(ipAddress).isSiteLocalAddress
    }.getOrDefault(false)

    fun getLocalIPv4DevicesAddresses() = ArrayList<InetAddress>().apply localIp4Devices@{
        runCatching {
            Timber.d("Getting all the local IPv4 addresses")
            Collections.list(NetworkInterface.getNetworkInterfaces()).forEach { networkInterface ->
                Collections.list(networkInterface.inetAddresses).forEach { inetAddress ->
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        Timber.d("Found a local IPv4 address $inetAddress")
                        this@localIp4Devices.add(inetAddress)
                    } else Unit
                }
            }
        }.onFailure {
            Timber.e("error getting the device IP address ${it.localizedMessage}")
        }
    }

    fun Long.millisToSec() = TimeUnit.MILLISECONDS.toSeconds(this)
}
