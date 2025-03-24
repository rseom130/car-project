package com.kimsiso.carproject.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kimsiso.carproject.GpsManager
import com.kimsiso.carproject.R

class GpsFragment : Fragment() {

    // GpsManager 클래스 선언
    private lateinit var gpsManager: GpsManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_gps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // GpsManager 초기화 및 UI 요소 바인딩
        gpsManager = GpsManager(requireContext())
        gpsManager.bindViews(
            view.findViewById(R.id.gpsTextView),
            view.findViewById(R.id.gpsSignalTextView),
            view.findViewById(R.id.gpsTestTextView)
        )
        gpsManager.initializePermissionLauncher(this)
        gpsManager.checkAndRequestPermission()
    }

    override fun onResume() {
        super.onResume()

        // GpsManager 시작
        gpsManager.startGpsUpdates()
    }

    override fun onPause() {
        super.onPause()

        // GpsManager 중지
        if (::gpsManager.isInitialized) { // 🔥 gpsManager가 초기화된 경우에만 실행
            gpsManager.stopGpsUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // GpsManager 중지
        if (::gpsManager.isInitialized) { // 🔥 gpsManager가 초기화된 경우에만 실행
            gpsManager.stopGpsUpdates()
        }
    }
}