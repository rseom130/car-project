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
import androidx.fragment.app.Fragment
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
    private var locationCallback: LocationCallback? = null // 🔥 Nullable (`?`) 추가
    private lateinit var locationManager: LocationManager

    private var lastLocation: Location? = null
    private var lastUpdateTime: Long = 0
    private var gpsSignalCount: Int = 0

    private var isGpsStarted = false // ✅ GPS가 이미 실행 중인지 확인
    private var gnssStatusCallback: GnssStatus.Callback? = null
    private var permissionLauncher: ActivityResultLauncher<String>? = null

    // UI 요소 바인딩
    fun bindViews(gpsTextView: TextView, gpsSignalTextView: TextView, gpsTestTextView: TextView) {
        this.gpsTextView = gpsTextView
        this.gpsSignalTextView = gpsSignalTextView
        this.gpsTestTextView = gpsTestTextView
    }

    fun initializePermissionLauncher(fragment: Fragment) {
        permissionLauncher = fragment.registerForActivityResult(
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
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher?.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startGpsUpdates()
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

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
            registerGnssStatusListener() // ✅ GPS 위성 리스너 등록
            isGpsStarted = true // ✅ GPS 시작 상태 변경
        }
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
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
        unregisterGnssStatusListener()
        isGpsStarted = false // ✅ GPS 중지 상태로 변경
    }

    private fun unregisterGnssStatusListener() {
        gnssStatusCallback?.let {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.unregisterGnssStatusCallback(it)
            gnssStatusCallback = null
        }
    }
}