package com.abiots.networktools

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.abiots.networktools.databinding.ActivityMainBinding
import com.abiots.networkutils.core.SubnetDevices
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var ipList: ArrayList<String?> = ArrayList()
    private val coroutineScope = CoroutineScope(Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        iniSubnetListener()

        coroutineScope.launch {
            SubnetDevices.fetchIPv4SubnetDevices()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("TEST", "Cancelling the coroutines")
        coroutineScope.coroutineContext.cancelChildren()
    }

    private fun iniSubnetListener() = coroutineScope.launch {
        SubnetDevices.getSubnetDevicesFlow().collectLatest { set ->
            Log.d("TEST", "${Thread.currentThread().name} Getting the final flow data ${set.size}")
            binding.textP.text = "\n\n final size ${set.size}"
            set.forEach { device ->
                binding.textP.append("\n\n\t IP address ${device.ip} is reachable ${device.macAddress}")
            }
        }
    }
}