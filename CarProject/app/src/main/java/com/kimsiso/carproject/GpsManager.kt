package com.kimsiso.carproject

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlin.math.roundToInt

class GpsManager(
    private val context: Context
) {
    private lateinit var gpsTextView: TextView
    private lateinit var gpsSignalTextView: TextView
    private lateinit var gpsTestTextView: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationManager: LocationManager
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private var lastLocation: Location? = null
    private var lastUpdateTime: Long = 0
    private var gpsSignalCount: Int = 0

    // UI 요소 바인딩
    fun bindViews(gpsTextView: TextView, gpsSignalTextView: TextView, gpsTestTextView: TextView) {
        this.gpsTextView = gpsTextView
        this.gpsSignalTextView = gpsSignalTextView
        this.gpsTestTextView = gpsTestTextView
    }

    fun initializePermissionLauncher(activity: androidx.activity.ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startGpsUpdates()
            } else {
                Toast.makeText(context, "GPS 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkAndRequestPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startGpsUpdates()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun startGpsUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
            .setMinUpdateIntervalMillis(500)
            .setWaitForAccurateLocation(true)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                updateSpeed(locationResult)
            }
        }

        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        registerGnssStatusListener()
    }

    @SuppressLint("MissingPermission")
    private fun registerGnssStatusListener() {
        val gnssStatusCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                super.onSatelliteStatusChanged(status)

                val satelliteCount = status.satelliteCount
                var strongSignalCount = 0
                val satelliteInfo = StringBuilder()
                gpsSignalCount = 0

                for (i in 0 until satelliteCount) {
                    val cn0 = status.getCn0DbHz(i)
                    val usedInFix = status.usedInFix(i)
                    if (usedInFix) {
                        gpsSignalCount++
                    }
                    if (cn0 > 30) {
                        strongSignalCount++
                    }

                    satelliteInfo.append("위성 $i: 신호 강도 = ${cn0} dB-Hz, 사용됨 = $usedInFix\n")
                }

                gpsSignalTextView.text = "위성 수: $satelliteCount\n강한 위성 수(30dB-Hz 이상): $strongSignalCount\n위성 정보:\n$satelliteInfo"
            }
        }

        locationManager.registerGnssStatusCallback(gnssStatusCallback)
    }

    private fun updateSpeed(locationResult: LocationResult) {
        val currentLocation = locationResult.lastLocation ?: return
        val currentTime = System.currentTimeMillis()

        lastLocation?.let {
            val distance = it.distanceTo(currentLocation)
            val timeDiff = (currentTime - lastUpdateTime) / 1000.0

            if (timeDiff > 0) {
                val speedKmh = (distance / timeDiff) * 3.6
                var speedKmhRounded = speedKmh.roundToInt()

                if (gpsSignalCount <= 0) {
                    speedKmhRounded = 0
                }

                gpsTextView.text = "$speedKmhRounded Km/h"
            }

            gpsTestTextView.text = "distance: $distance\ncurrentTime: $currentTime\ntimeDiff: $timeDiff\nKMH: ${(distance / timeDiff) * 3.6}"
        }

        lastLocation = currentLocation
        lastUpdateTime = currentTime
    }

    fun stopGpsUpdates() {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}