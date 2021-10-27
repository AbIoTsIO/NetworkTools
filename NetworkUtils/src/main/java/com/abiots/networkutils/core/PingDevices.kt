package com.abiots.networkutils.core

import com.abiots.networkutils.BuildConfig
import com.abiots.networkutils.model.PingResult
import com.abiots.networkutils.utils.NetworkUtils.millisToSec
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import timber.log.Timber
import java.net.InetAddress
import kotlin.system.measureTimeMillis

/**
 * @author J Suhas Bhat
 */
object PingDevices {

    init {
        if (Timber.treeCount() == 0 && BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else Unit
    }

    private const val PING_TIME_OUT_IN_SEC = 1
    private const val PING_TTL = 128
    private const val PING_COMMAND = "ping"

    /**
     * This method is used to perform the Native Ping, which runs the Ping command using the android process.
     * @param hostAddress InetAddress- Address we need to ping
     * @return PingResult
     */
    suspend fun doNativePing(hostAddress: InetAddress) = withContext(IO) {
        Timber.d(" ${Thread.currentThread().name} :- Native pinging the address ${hostAddress.hostAddress}")
        PingResult(hostAddress).apply result@{
            runCatching {
                val pingProc = Runtime.getRuntime()
                    .exec("$PING_COMMAND -c 1 -W $PING_TIME_OUT_IN_SEC -t $PING_TTL ${hostAddress.hostAddress}")
                pingProc.waitFor()
                pingProc.let {
                    when (it.exitValue()) {
                        0 -> {
                            handlePingResponse(
                                pingResult = this@result,
                                pingResponse = it.inputStream.bufferedReader().readLines()
                                    .toString()
                            )
                        }
                        1 -> this@result.error = "failed, exit = 1"
                        else -> this@result.error = "error, exit = 2"
                    }
                }
                this@result
            }.onFailure {
                this@result.error = it.localizedMessage
            }.onSuccess {
                Timber.d("Ping result ${this.inetAddress} is reachable:${this.isReachable}, time taken: ${this.timeTaken} seconds")
            }
        }
    }

    /**
     * This method is to perform the Java ping.
     */
    suspend fun doJavaPing(host: InetAddress, pingResult: PingResult) = withContext(IO) {
        Timber.d("  ${Thread.currentThread().name} :- Performing the java ping to the address ${host.hostAddress}")
        measureTimeMillis {
            runCatching {
                host.isReachable(PING_TIME_OUT_IN_SEC)
            }.onFailure {
                pingResult.error = "Timed Out: ${it.localizedMessage}"
            }.onSuccess {
                pingResult.isReachable = it
                if (!pingResult.isReachable) pingResult.error = "Timed Out" else Unit
            }
        }.also {
            Timber.d("got java ping result after $it milli seconds")
            pingResult.timeTaken = it.millisToSec().toFloat()
        }
    }

    private fun handlePingResponse(pingResult: PingResult, pingResponse: String) {
        if (pingResponse.contains("0% packet loss")) {
            val start: Int = pingResponse.indexOf("/mdev = ")
            val end: Int = pingResponse.indexOf(" ms]", start)
            if (start == -1 || end == -1) {
                pingResult.error = "Error: $pingResponse"
            } else {
                pingResponse.substring(start + 8, end).let {
                    val stats: Array<String> = it.split("/".toRegex()).toTypedArray()
                    pingResult.isReachable = true
                    pingResult.result = pingResponse
                    pingResult.timeTaken = stats[1].toFloat()
                }
            }
        } else if (pingResponse.contains("100% packet loss")) {
            pingResult.error = "100% packet loss"
        } else if (pingResponse.contains("% packet loss")) {
            pingResult.error = "partial packet loss"
        } else if (pingResponse.contains("unknown host")) {
            pingResult.error = "unknown host"
        } else {
            pingResult.error = "unknown error in getPingStats"
        }
    }
}