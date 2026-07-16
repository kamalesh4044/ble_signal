package com.example.bluetoothaudio

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvDeviceList: TextView
    private lateinit var btnStartServer: Button
    private lateinit var btnScan: Button

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isScanning = false
    private val scannedDevices = mutableMapOf<String, String>()
    
    // A2DP Sink UUID (Standard Bluetooth Audio Receiver)
    private val A2DP_SINK_UUID = ParcelUuid(UUID.fromString("0000110B-0000-1000-8000-00805F9B34FB"))

    private val REQUIRED_PERMS = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvDeviceList = findViewById(R.id.tvDeviceList)
        btnStartServer = findViewById(R.id.btnStartServer)
        btnScan = findViewById(R.id.btnScan)

        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bm.adapter

        btnStartServer.text = "Advertise as Bluetooth Speaker"
        btnStartServer.setOnClickListener {
            if (checkAndRequestPermissions()) {
                startAdvertisingAsSpeaker()
                attemptA2dpSinkReflection()
            }
        }

        btnScan.setOnClickListener {
            if (checkAndRequestPermissions()) {
                if (isScanning) {
                    stopScanningForDevices()
                } else {
                    startScanningForDevices()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertisingAsSpeaker() {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            tvStatus.text = "Error: BLE Advertising not supported on this device."
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(A2DP_SINK_UUID)
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)
        tvStatus.text = "Status: Advertising as A2DP Sink (Speaker)..."
        Log.d("BTAudio", "Started advertising.")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            runOnUiThread {
                tvStatus.append("\nAdvertising Success! Check other phone's Bluetooth settings.")
            }
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            runOnUiThread {
                tvStatus.text = "Status: Advertising Failed. Code: $errorCode"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanningForDevices() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            tvStatus.text = "Error: BLE Scanner not available."
            return
        }
        
        scannedDevices.clear()
        updateDeviceListUI()
        
        scanner.startScan(scanCallback)
        isScanning = true
        btnScan.text = "Stop Scanning"
        tvStatus.text = "Status: Scanning for nearby devices..."
    }

    @SuppressLint("MissingPermission")
    private fun stopScanningForDevices() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.stopScan(scanCallback)
        isScanning = false
        btnScan.text = "Scan for Nearby Devices"
        tvStatus.text = "Status: Scan stopped. Found ${scannedDevices.size} devices."
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            val device = result.device
            val address = device.address
            val name = device.name ?: "Unknown Device (Hidden)"
            val rssi = result.rssi
            
            val label = "[$address] $name (Strength: $rssi dBm)"
            
            // Prioritize named devices over unknown ones for the same MAC
            if (!scannedDevices.containsKey(address) || (name != "Unknown Device (Hidden)")) {
                scannedDevices[address] = label
                updateDeviceListUI()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            runOnUiThread {
                tvStatus.text = "Status: Scan Failed. Code: $errorCode"
            }
        }
    }

    private fun updateDeviceListUI() {
        runOnUiThread {
            if (scannedDevices.isEmpty()) {
                tvDeviceList.text = "Listening for signals..."
            } else {
                val sb = StringBuilder()
                sb.append("Total Devices Found: ${scannedDevices.size}\n\n")
                
                // Sort by name so known devices appear at the top
                val sorted = scannedDevices.values.sortedBy { it.contains("Unknown") }
                for (deviceStr in sorted) {
                    sb.append(deviceStr).append("\n\n")
                }
                tvDeviceList.text = sb.toString()
            }
        }
    }

    private fun attemptA2dpSinkReflection() {
        try {
            val a2dpSinkClass = Class.forName("android.bluetooth.BluetoothA2dpSink")
            runOnUiThread {
                tvStatus.append("\nReflection: Found BluetoothA2dpSink class.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                tvStatus.append("\nReflection Failed: ${e.message}")
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        for (p in REQUIRED_PERMS) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMS, 101)
                return false
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        if (isScanning) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
    }
}
