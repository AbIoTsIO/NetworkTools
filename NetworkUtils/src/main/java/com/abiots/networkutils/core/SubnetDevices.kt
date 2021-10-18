package com.abiots.networkutils.core

import com.abiots.networkutils.BuildConfig
import com.abiots.networkutils.model.Device
import com.abiots.networkutils.utils.NetworkUtils
import com.abiots.networkutils.utils.NetworkUtils.millisToSec
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import java.net.InetAddress
import kotlin.system.measureTimeMillis

object SubnetDevices {

    init {
        if (Timber.treeCount() == 0 && BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else Unit
    }

    var reachableSubnetDevices = MutableStateFlow<ArrayList<Device>>(ArrayList())

    suspend fun fetchIPv4SubnetDevices() = withContext(Default) {
        Timber.d("${Thread.currentThread().name} getting all the subnet devices")
        ArrayList<Device>().apply subnetDevices@{
            measureTimeMillis {
                coroutineScope {
                    NetworkUtils.getLocalIPv4DevicesAddresses()[0].getSubnetIpAddresses()
                        .forEach { ip4Address ->
                            async(Default) pingAsync@{
                                runCatching {
                                    InetAddress.getByName(ip4Address).let {
                                       val pingResult= PingDevices.doNativePing(it)
                                        if (!pingResult.isReachable) {
                                            PingDevices.doJavaPing(host = it, pingResult)
                                        } else Unit
                                        pingResult
                                    }
                                }.onFailure {
                                    Timber.e("Failed to ping $ip4Address: ${it.localizedMessage}")
                                }.onSuccess {
                                    if (it.isReachable) this@subnetDevices.add(it.toDevice())
                                    else Unit
                                }
                            }.start()
                        }
                }
            }.also {
                Timber.d(" ${Thread.currentThread().name} :- got a total of ${this@subnetDevices.size} subnet devices after ${it.millisToSec()} seconds")
                reachableSubnetDevices.value = this@subnetDevices
            }
        }
    }

    private suspend fun InetAddress.getSubnetIpAddresses() =
        ArrayList<String>().apply subnetDevices@{
            Timber.d(" ${Thread.currentThread().name} :- Getting the subnet addresses from local IP")
            this@getSubnetIpAddresses.hostAddress?.let {
                it.substring(0, it.lastIndexOf(".") + 1).let { subnet ->
                    AddressResolutionProtocol.getAllIPAndMACAddressesInARPCache().keys.forEach { ipAddress ->
                        if (ipAddress.startsWith(subnet)) {
                            this@subnetDevices.add(ipAddress)
                        } else Unit
                    }
                    for (i in 0..255)
                        if (!this@subnetDevices.contains(subnet + i)) this@subnetDevices.add(subnet + i)
                }
            }
        }
}
