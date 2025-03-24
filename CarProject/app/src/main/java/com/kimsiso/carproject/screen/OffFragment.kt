package com.kimsiso.carproject.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kimsiso.carproject.R
import com.kimsiso.carproject.SystemManager

class OffFragment : Fragment() {

    // SystemManager 클래스 선언
    private lateinit var systemManager: SystemManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_off, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ✅ SystemManager 초기화 및 UI 요소 바인딩
        systemManager = SystemManager(requireContext())
        systemManager.bindViews(
            view.findViewById(R.id.dateTextView),
            view.findViewById(R.id.timeTextView),
            view.findViewById(R.id.betteryTextView)
        )
    }

    override fun onPause() {
        super.onPause()

        // SystemManager 중지
        if (::systemManager.isInitialized) {
            systemManager.destroy() // ✅ 시스템 관리 정리
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // SystemManager 중지
        if (::systemManager.isInitialized) {
            systemManager.destroy() // ✅ 시스템 관리 정리
        }
    }
}