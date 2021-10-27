package com.abiots.networkutils.core

import com.abiots.networkutils.BuildConfig
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileReader

/**
 * @author J Suhas Bhat
 * - ARP stands for Address Resolution Protocol
 */
object AddressResolutionProtocol {

    init {
        if (Timber.treeCount() == 0 && BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else Unit
    }

    private const val GET_IP_NEIGHBOURS_COMMAND = "ip neigh show"
    private const val ARP_CACHE_PATH = "/proc/net/arp"
    private const val INVALID_MAC_ADDRESS = "00:00:00:00:00:00"
    private val MAC_ID_REGEX = "..:..:..:..:..:..".toRegex()

    /**
     * This method is used to get the Ip address-Mac address map from the ARP cache and IPS sleigh.
     */
    suspend fun getAllIPAndMACAddressesInARPCache() = withContext(IO) {
        Timber.d("getting ip and mac data from ARP cache")
        getAllIpAndMACAddressesFromIPSleigh().apply ipMacMap@{
            runCatching {
                if (File(ARP_CACHE_PATH).canRead()) {
                    FileReader(ARP_CACHE_PATH).buffered().readLines().forEach { arpData ->
                        arpData.split(" +").let {
                            if (it.size >= 4 && it[3].matches(MAC_ID_REGEX) && it[3] != INVALID_MAC_ADDRESS && !this@ipMacMap.containsKey(it[0])) {
                                this@ipMacMap.put(it[0], it[3])
                            } else Unit
                        }
                    }
                } else Unit
            }.onFailure {
                Timber.e("Error opening the ARP data cache %s", it.localizedMessage)
            }.onSuccess {
                Timber.d("ARP data from the ARP cache are :${this@ipMacMap} ")
            }
        }
    }

    private suspend fun getAllIpAndMACAddressesFromIPSleigh() = withContext(IO) {
        Timber.d("Getting IP and MAC from ARP IP sleigh")
        HashMap<String, String>().apply macIpHashMap@{
            runCatching {
                Timber.d("getting the IP and mac from the IP sleigh")
                val ipNeighboursProcess = Runtime.getRuntime().exec(GET_IP_NEIGHBOURS_COMMAND)
                ipNeighboursProcess.waitFor()
                ipNeighboursProcess.inputStream.bufferedReader().readLines()
            }.onFailure {
                Timber.e("error getting the IP and mac address from the ARP IP sleigh ${it.localizedMessage}")
            }.onSuccess {
                Timber.d("ARP data from Ip sleigh $it")
                it.forEach { line ->
                    line.split(" ").let { details ->
                        if (details.size > 4) {
                            this@macIpHashMap.put(details[0], details[4])
                        } else Unit
                    }
                }
            }
        }
    }
}