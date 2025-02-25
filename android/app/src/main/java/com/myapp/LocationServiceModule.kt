package com.myapp

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class LocationServiceModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val PERMISSION_REQUEST_CODE = 100
    }

    private var locationUpdateReceiver: BroadcastReceiver? = null

    override fun getName(): String {
        return "LocationServiceModule"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun initialize() {
        super.initialize()
        // Register a broadcast receiver to listen for location updates from the service.
        val filter = IntentFilter("com.myapp.LOCATION_UPDATE")
        locationUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val latitude = it.getDoubleExtra("latitude", 0.0)
                    val longitude = it.getDoubleExtra("longitude", 0.0)
                    val map = Arguments.createMap().apply {
                        putDouble("latitude", latitude)
                        putDouble("longitude", longitude)
                    }
                    sendEvent("onLocationUpdate", map)
                }
            }
        }

        reactApplicationContext.registerReceiver(locationUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

    }

    @ReactMethod
    fun start() {
        val currentActivity: Activity? = currentActivity
        val context = reactApplicationContext

        if (LocationService.isServiceRunning) {
            sendEvent("onStatusChanged", true)
            return
        }

        if (currentActivity == null) {
            return
        }

        // Check permissions
        val fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                currentActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
            // Optionally, wait for permissions before starting the service.
        }

        // For Android 13+ (SDK 34) check foreground service location permission.
        if (Build.VERSION.SDK_INT >= 34) {
            val fgPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            if (fgPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    currentActivity,
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
            }
        }

        // Start the foreground service.
        val serviceIntent = Intent(context, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Immediately emit status event.
        sendEvent("onStatusChanged", true)
    }

    @ReactMethod
    fun stop() {
        reactApplicationContext.stopService(Intent(reactApplicationContext, LocationService::class.java))
        sendEvent("onStatusChanged", false)
    }

    private fun sendEvent(eventName: String, params: Any?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
}
