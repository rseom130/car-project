package com.kimsiso.carproject.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kimsiso.carproject.GpsManager
import com.kimsiso.carproject.MusicController
import com.kimsiso.carproject.R
import com.kimsiso.carproject.SystemManager

class HomeFragment : Fragment() {
    // MusicController 클래스 선언
    private lateinit var musicController: MusicController

    // GpsManager 클래스 선언
    private lateinit var gpsManager: GpsManager

    // SystemManager 클래스 선언
    private lateinit var systemManager: SystemManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // MusicController 초기화 및 바인딩
        musicController = MusicController(requireContext())
        musicController.bindViews(
            view.findViewById(R.id.titleTextView),
            view.findViewById(R.id.artistTextView),
            view.findViewById(R.id.albumImageView),
            view.findViewById(R.id.playButton),
            view.findViewById(R.id.nextButton),
            view.findViewById(R.id.prevButton)
        )

        // GpsManager 초기화 및 UI 요소 바인딩
        gpsManager = GpsManager(requireContext())
        gpsManager.bindViews(
            view.findViewById(R.id.gpsTextView),
            view.findViewById(R.id.gpsSignalTextView),
            view.findViewById(R.id.gpsTestTextView)
        )
        gpsManager.initializePermissionLauncher(this)
        gpsManager.checkAndRequestPermission()

        // ✅ SystemManager 초기화 및 UI 요소 바인딩
        systemManager = SystemManager(requireContext())
        systemManager.bindViews(
            view.findViewById(R.id.dateTextView),
            view.findViewById(R.id.timeTextView),
            view.findViewById(R.id.betteryTextView)
        )
    }

    override fun onResume() {
        super.onResume()

        // MusicController 리시버 등록
        musicController.registerReceiver()
        val intent = Intent("REQUEST_MUSIC_INFO")
        requireContext().sendBroadcast(intent)

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