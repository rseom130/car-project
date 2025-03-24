package com.kimsiso.carproject

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import android.Manifest
import android.annotation.SuppressLint
import android.location.GnssStatus
import android.location.LocationManager
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    // MusicController 클래스 선언
    private lateinit var musicController: MusicController

    // GpsManager 클래스 선언
    private lateinit var gpsManager: GpsManager

    // SystemManager 클래스 선언
    private lateinit var systemManager: SystemManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 전체화면 모드 적용
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val controller = view.windowInsetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            view.onApplyWindowInsets(insets)
        }

        // 화면 켜짐 유지
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // MusicController 초기화 및 바인딩
        musicController = MusicController(this)
        musicController.bindViews(
            findViewById(R.id.titleTextView),
            findViewById(R.id.artistTextView),
            findViewById(R.id.albumImageView),
            findViewById(R.id.playButton),
            findViewById(R.id.nextButton),
            findViewById(R.id.prevButton)
        )

        gpsManager = GpsManager(this)
        gpsManager.bindViews(
            findViewById(R.id.gpsTextView),
            findViewById(R.id.gpsSignalTextView),
            findViewById(R.id.gpsTestTextView)
        )
        gpsManager.initializePermissionLauncher(this)
        gpsManager.checkAndRequestPermission()

        // ✅ SystemManager 초기화 및 UI 요소 바인딩
        systemManager = SystemManager(this)
        systemManager.bindViews(
            findViewById(R.id.dateTextView),
            findViewById(R.id.timeTextView),
            findViewById(R.id.betteryTextView),
            findViewById(R.id.refreshButton)
        )
    }

    override fun onResume() {
        super.onResume()

        // MusicController 리시버 등록
        musicController.registerReceiver()
        val intent = Intent("REQUEST_MUSIC_INFO")
        sendBroadcast(intent)

        // GpsManager 시작
        gpsManager.startGpsUpdates()
    }

    override fun onPause() {
        super.onPause()

        // musicController 리시버 종료
        if (::musicController.isInitialized) {
            musicController.unregisterReceiver()
        }

        // GpsManager 중지
        if (::gpsManager.isInitialized) { // 🔥 gpsManager가 초기화된 경우에만 실행
            gpsManager.stopGpsUpdates()
        }

        // SystemManager 중지
        if (::systemManager.isInitialized) {
            systemManager.destroy() // ✅ 시스템 관리 정리
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // musicController 리시버 종료
        if (::musicController.isInitialized) {
            musicController.unregisterReceiver()
        }
        // GpsManager 중지
        if (::gpsManager.isInitialized) { // 🔥 gpsManager가 초기화된 경우에만 실행
            gpsManager.stopGpsUpdates()
        }

        // SystemManager 중지
        if (::systemManager.isInitialized) {
            systemManager.destroy() // ✅ 시스템 관리 정리
        }
    }

}