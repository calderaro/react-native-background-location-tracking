package com.myapp

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class LocationServiceModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val PERMISSION_REQUEST_CODE = 100
        var trackingToken: String? = null
    }

    private var locationUpdateReceiver: BroadcastReceiver? = null
    private val httpClient = OkHttpClient()

    override fun getName(): String {
        return "LocationServiceModule"
    }

    override fun initialize() {
        super.initialize()
        // Register the broadcast receiver to listen for location updates.
        val filter = IntentFilter("com.myapp.LOCATION_UPDATE")
        locationUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val lat = intent.getDoubleExtra("latitude", 0.0)
                val lon = intent.getDoubleExtra("longitude", 0.0)
                val params = Arguments.createMap().apply {
                    putDouble("latitude", lat)
                    putDouble("longitude", lon)
                }
                // Emit the event to JavaScript.
                sendEvent("onLocationUpdate", params)

                //Post to server
                postLocationToServer(lat, lon)
            }
        }

        ContextCompat.registerReceiver(
            reactApplicationContext,
            locationUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    @ReactMethod
    fun start(token: String) {
        val currentActivity: Activity? = currentActivity
        val context = reactApplicationContext

        if (currentActivity == null) {
            return
        }

        trackingToken = token;

        if (LocationService.isServiceRunning) {
            sendEvent("onStatusChanged", true)
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
        trackingToken = null
    }

    private fun sendEvent(eventName: String, params: Any?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun postLocationToServer(latitude: Double, longitude: Double) {
        // Replace with your actual server URL.
        val url = "https://neat-ant-94.deno.dev/points"
        val json = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            trackingToken?.let {
                put("token", it)
            }
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(body).build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("LocationServiceModule", "Post to server result: ${response.code}")
                response.close()
            }
        })
    }
}
