package com.example.bluetoothaudio

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
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
    private lateinit var btnStartServer: Button
    private lateinit var btnScan: Button

    private var bluetoothAdapter: BluetoothAdapter? = null
    
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
            tvStatus.text = "Status: Scan disabled for this prototype."
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

    private fun attemptA2dpSinkReflection() {
        try {
            // Attempt to get the hidden BluetoothA2dpSink class
            val a2dpSinkClass = Class.forName("android.bluetooth.BluetoothA2dpSink")
            
            // In a real system app, we would register a profile listener for A2dpSink.
            // Since this is restricted, we'll just log if we can even see the class.
            runOnUiThread {
                tvStatus.append("\nReflection: Found BluetoothA2dpSink class.")
            }
            Log.d("BTAudio", "Successfully reflected android.bluetooth.BluetoothA2dpSink")
            
            // NOTE: Actually instantiating it and registering it as an active profile
            // requires BLUETOOTH_PRIVILEGED (System app) or rooted access.
            // Any further reflection calls to connect/accept will throw SecurityExceptions.
            
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
    }
}
