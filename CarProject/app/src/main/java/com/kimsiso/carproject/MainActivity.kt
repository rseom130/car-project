package com.kimsiso.carproject

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.*
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.kimsiso.carproject.screen.GpsFragment
import com.kimsiso.carproject.screen.HomeFragment
import com.kimsiso.carproject.screen.MusicFragment
import com.kimsiso.carproject.screen.OffFragment
import com.kimsiso.carproject.screen.PictureFragment


class MainActivity : AppCompatActivity() {
    private lateinit var homeButton : ImageButton
    private lateinit var pictureButton : ImageButton
    private lateinit var gpsButton : ImageButton
    private lateinit var musicButton : ImageButton
    private lateinit var offButton : ImageButton

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 전체화면 모드 적용
        enableFullScreenMode()

        // ✅ 현재 화면이 가로 모드인지 확인
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            // 가로 모드 (왼쪽 메뉴 사용)
            homeButton = findViewById(R.id.homeButton2)
            pictureButton = findViewById(R.id.pictureButton2)
            gpsButton = findViewById(R.id.gpsButton2)
            musicButton = findViewById(R.id.musicButton2)
            offButton = findViewById(R.id.offButton2)
        } else {
            // 세로 모드 (하단 메뉴 사용)
            homeButton = findViewById(R.id.homeButton)
            pictureButton = findViewById(R.id.pictureButton)
            gpsButton = findViewById(R.id.gpsButton)
            musicButton = findViewById(R.id.musicButton)
            offButton = findViewById(R.id.offButton)
        }

        // 화면 켜짐 유지
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 초기 화면 설정 (홈 화면)
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            updateButtonTint(homeButton)
        }

        homeButton.setOnClickListener({
            replaceFragment(HomeFragment())
            updateButtonTint(homeButton)
        })
        pictureButton.setOnClickListener {
            replaceFragment(PictureFragment())
            updateButtonTint(pictureButton)
        }

        gpsButton.setOnClickListener {
            replaceFragment(GpsFragment())
            updateButtonTint(gpsButton)
        }

        musicButton.setOnClickListener {
            replaceFragment(MusicFragment())
            updateButtonTint(musicButton)
        }

        offButton.setOnClickListener {
            replaceFragment(OffFragment())
            updateButtonTint(offButton)
        }
    }

    // ✅ 소프트키 높이만큼 패딩을 자동 조절하는 함수 (공통)
    private fun applyBottomPaddingForNavigationView(view: View) {
        view.setOnApplyWindowInsetsListener { v, insets ->
            val systemBars = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom) // ✅ 소프트키 높이만큼 자동 조절
            insets
        }
    }

    private fun enableFullScreenMode() {
        window.decorView.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }
    }

    // ✅ Fragment 변경 메서드
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .commit()
    }

    // ✅ 버튼 Tint 색상 업데이트 메서드
    private fun updateButtonTint(selectedButton: ImageButton) {
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val grayColor = ContextCompat.getColor(this, R.color.gray)

        // 모든 버튼을 grayColor로 변경
        listOf(homeButton, pictureButton, gpsButton, musicButton, offButton).forEach { button ->
            button.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN)
        }

        // 클릭된 버튼만 whiteColor로 변경
        selectedButton.setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN)
    }
}