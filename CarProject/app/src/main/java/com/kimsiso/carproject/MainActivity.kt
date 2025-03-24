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
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private lateinit var albumeImageView: ImageView
    private lateinit var artistTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var playButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView

    private lateinit var gpsTextView: TextView
    private lateinit var gpsTestTextView: TextView
    private lateinit var gpsSignalTextView: TextView
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var lastUpdateTime: Long = 0
    private var gpsSignalCount: Int = 0

    private lateinit var batteryTextView: TextView

    private lateinit var refreshButton: Button

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

        // 알림 리스너 서비스 접근 권한 확인
        if (!isNotificationServiceEnabled(this)) {
//            Log.d("MusicInfo", "🚨 알림 접근 권한 없음. 설정 화면으로 이동.")
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
//            Log.d("MusicInfo", "✅ 알림 접근 권한이 활성화됨.")
        }

        // 요소 정의
        albumeImageView = findViewById(R.id.albumImageView)
        artistTextView = findViewById(R.id.artistTextView)
        titleTextView = findViewById(R.id.titleTextView)
        playButton = findViewById(R.id.playButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        dateTextView = findViewById(R.id.dateTextView)
        timeTextView = findViewById(R.id.timeTextView)
        refreshButton = findViewById(R.id.refreshButton)
        batteryTextView = findViewById(R.id.betteryTextView)

        refreshButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            finish() // 현재 액티비티 종료
            startActivity(intent) // 새로운 액티비티 시작
        }

        // 🎵 현재 실행 중인 음악 앱의 미디어 세션 가져오기
        setupMediaController()

        // ▶ 재생 / ⏸ 일시정지 버튼 클릭 이벤트
        playButton.setOnClickListener {
            togglePlayPause()
        }

        // ⏭ 다음 곡 버튼 클릭 이벤트
        nextButton.setOnClickListener {
            skipToNextTrack()
        }

        // ⏮ 이전 곡 버튼 클릭 이벤트
        prevButton.setOnClickListener {
            skipToPreviousTrack()
        }

        // 날짜 시간 업데이트
        updateDateTime()
        handler.postDelayed(updateTimeRunnable, 1000)

        // GPS
        gpsTextView = findViewById(R.id.gpsTextView)
        gpsSignalTextView = findViewById(R.id.gpsSignalTextView)
        gpsTestTextView = findViewById(R.id.gpsTestTextView)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 🔥 핸드폰 GPS를 무조건 사용하도록 설정
        locationRequest = LocationRequest.Builder(500) // 0.5초 간격 업데이트
            .setMinUpdateIntervalMillis(500) // 최소 0.5초 간격 업데이트
            .setWaitForAccurateLocation(true) // 정밀한 위치 기다리기
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY) // 높은 정확도
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                updateSpeed(locationResult)
            }
        }

        // 위성 정보
        registerGnssStatusListener()

        // 배터리 정보
        registerBatteryReceiver()
    }

    @SuppressLint("MissingPermission")
    private fun registerGnssStatusListener() {
        val gnssStatusCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                super.onSatelliteStatusChanged(status)

                val satelliteCount = status.satelliteCount // 현재 수신 중인 위성 개수
                var strongSignalCount = 0 // 강한 신호의 위성 개수
                val satelliteInfo = StringBuilder()
                gpsSignalCount = 0

                for (i in 0 until satelliteCount) {
                    val cn0 = status.getCn0DbHz(i) // 신호 강도 (dB-Hz)
                    val usedInFix = status.usedInFix(i) // 현재 위치 계산에 사용 중인지 여부
                    if(usedInFix) {
                        gpsSignalCount++
                    }

                    if (cn0 > 30) { // 신호 강도가 30dB-Hz 이상이면 강한 신호로 간주
                        strongSignalCount++
                    }

                    satelliteInfo.append("위성 $i: 신호 강도 = ${cn0} dB-Hz, 사용됨 = $usedInFix\n")
                }

//                println("🔹 현재 GPS 위성 개수: $satelliteCount")
//                println("🔹 강한 신호의 위성 개수 (30dB-Hz 이상): $strongSignalCount")
//                println("🔹 위성 정보: \n$satelliteInfo")
                gpsSignalTextView.text = "위성 수 : $satelliteCount\n강한 위성 수(30db-Hz 이상) : $strongSignalCount\n위성 정보 : \n$satelliteInfo"
            }
        }

        locationManager.registerGnssStatusCallback(gnssStatusCallback)
    }

    private fun updateSpeed(locationResult: LocationResult) {
        val currentLocation = locationResult.lastLocation ?: return
        val currentTime = System.currentTimeMillis()

        // 0.5초 전 위치가 있다면 속도 계산
        lastLocation?.let {
            val distance = it.distanceTo(currentLocation) // 두 위치 간 거리(m)
            val timeDiff = (currentTime - lastUpdateTime) / 1000.0 // 초 단위 변환

            var speedMpsTemp = distance / timeDiff // 속도 (m/s)
            var speedKmhTemp = speedMpsTemp * 3.6 // 속도 (km/h) 변환
            gpsTestTextView.text = "distance : $distance\ncurrentTime : $currentTime\ntimeDiff : $timeDiff\nMPS : $speedMpsTemp\nKMH : $speedKmhTemp"

            if (timeDiff > 0) {
                var speedMps = distance / timeDiff // 속도 (m/s)
                val speedKmh = speedMps * 3.6 // 속도 (km/h) 변환
                var speedKmh_con = 0
                speedKmh_con = speedKmh.roundToInt()

                if(gpsSignalCount<=0) {
                    speedKmh_con = 0
                }
                gpsTextView.text = "$speedKmh_con Km/h"
//                println("계산된 속도: $speedKmh_con km/h")
            }
        }

        // 현재 위치를 저장하여 다음 업데이트에 활용
        lastLocation = currentLocation
        lastUpdateTime = currentTime
    }

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val cn = ComponentName(context, MusicNotificationListener::class.java)
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(cn.flattenToString()) ?: false
    }

    // ✅ UI 업데이트를 위한 BroadcastReceiver
    private val uiUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "MUSIC_INFO_UPDATE") {
                val title = intent.getStringExtra("title") ?: "제목 없음"
                val artist = intent.getStringExtra("artist") ?: "가수 없음"
                val albumByteArray = intent.getByteArrayExtra("album")

                var albumArt: Bitmap? = null
                if (albumByteArray != null) {
                    albumArt = BitmapFactory.decodeByteArray(albumByteArray, 0, albumByteArray.size)
                }

                // ✅ UI 업데이트
                runOnUiThread {
                    titleTextView.text = title
                    artistTextView.text = artist
                    if (albumArt != null) {
                        albumeImageView.setImageBitmap(albumArt)
                    } else {
                        albumeImageView.setImageResource(R.drawable.ic_launcher_background)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        Log.d("MusicInfo-Main", "onResume")
        val filter = IntentFilter("MUSIC_INFO_UPDATE")

        // 📌 Android 13(API 33) 이상에서는 `Context.RECEIVER_NOT_EXPORTED` 필수
        registerReceiver(uiUpdateReceiver, filter, Context.RECEIVER_EXPORTED)

        val intent = Intent("REQUEST_MUSIC_INFO")
        sendBroadcast(intent)
//        Log.d("MainActivity", "🔄 기존 음악 정보를 요청함")

//        GPS
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onPause() {
        super.onPause()
//        Log.d("MusicInfo-Main", "onPause")
        unregisterReceiver(uiUpdateReceiver)
//        GPS
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 🎵 현재 실행 중인 미디어 세션 가져오기
    private fun setupMediaController() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mediaSessionManager = getSystemService(MediaSessionManager::class.java)
            val mediaSessions = mediaSessionManager.getActiveSessions(
                ComponentName(this, MusicNotificationListener::class.java)
            )

            if (mediaSessions.isNotEmpty()) {
                mediaController = mediaSessions[0] // 첫 번째 미디어 세션 사용
//                Log.d("MediaControl", "🎵 미디어 컨트롤러 설정 완료: ${mediaController?.packageName}")
                updatePlayButtonText() // ✅ 현재 상태에 맞게 버튼 텍스트 업데이트
            } else {
//                Log.d("MediaControl", "⚠️ 활성화된 미디어 세션이 없음")
            }
        }
    }

    // ▶ 재생 / ⏸ 일시정지 토글 기능
    private fun togglePlayPause() {
        mediaController?.let {
            val playbackState = it.playbackState
            if (playbackState != null) {
                when (playbackState.state) {
                    PlaybackState.STATE_PLAYING -> {
                        it.transportControls.pause()
//                        Log.d("MediaControl", "⏸ 음악 일시정지")
                        playButton.text = "PLAY"
                    }
                    PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED -> {
                        it.transportControls.play()
//                        Log.d("MediaControl", "▶ 음악 재생")
                        playButton.text = "PAUSE"
                    }
                    else -> Log.d("MediaControl", "⚠️ 미디어 상태 확인 불가")
                }
            }
        }
    }

    // ⏭ 다음 곡으로 스킵
    private fun skipToNextTrack() {
        mediaController?.let {
            it.transportControls.skipToNext()
//            Log.d("MediaControl", "⏭ 다음 곡")
            playButton.text = "PAUSE"
        }
    }

    // ⏮ 이전 곡으로 스킵
    private fun skipToPreviousTrack() {
        mediaController?.let {
            it.transportControls.skipToPrevious()
//            Log.d("MediaControl", "⏮ 이전 곡")
            playButton.text = "PAUSE"
        }
    }

    // 🔄 현재 미디어 상태에 맞게 playButton의 텍스트 업데이트
    private fun updatePlayButtonText() {
        mediaController?.let {
            val playbackState = it.playbackState
            if (playbackState != null) {
                playButton.text = when (playbackState.state) {
                    PlaybackState.STATE_PLAYING -> "PAUSE"
                    PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED -> "PLAY"
                    else -> "PLAY"
                }
            }
        }
    }

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000) // 1초마다 실행
        }
    }

    private fun updateDateTime() {
        val now = Date()
        dateTextView.text = dateFormat.format(now)
        timeTextView.text = timeFormat.format(now)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimeRunnable)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // 배터리
    private fun registerBatteryReceiver() {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) // 배터리 잔량 (%)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1) // 최대값 (보통 100)
                    if (level >= 0 && scale > 0) {
                        val batteryPct = (level * 100) / scale // 배터리 퍼센트 계산
                        batteryTextView.text = "배터리 잔량: $batteryPct%"
                    }
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

}