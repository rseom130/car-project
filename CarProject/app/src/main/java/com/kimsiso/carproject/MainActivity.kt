package com.kimsiso.carproject

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import android.content.res.Configuration
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.kimsiso.carproject.screen.GpsFragment
import com.kimsiso.carproject.screen.HomeFragment
import com.kimsiso.carproject.screen.MusicFragment
import com.kimsiso.carproject.screen.OffFragment
import com.kimsiso.carproject.screen.PictureFragment


@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity() {

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
            val sideNavigationView = findViewById<NavigationView>(R.id.side_navigation)
            val sideNavigationView2 = findViewById<NavigationView>(R.id.side_navigation2)
            applyBottomPaddingForNavigationView(sideNavigationView2)

            sideNavigationView.setNavigationItemSelectedListener { item ->
                handleNavigation(item.itemId)
                true
            }
        } else {
            // 세로 모드 (하단 메뉴 사용)
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            applyBottomPaddingForNavigationView(bottomNavigationView)

            bottomNavigationView.setOnItemSelectedListener { item ->
                handleNavigation(item.itemId)
                true
            }
        }


        // 화면 켜짐 유지
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 초기 화면 설정 (홈 화면)
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
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


    // ✅ 네비게이션 아이템 클릭 시 화면 변경
    private fun handleNavigation(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> replaceFragment(HomeFragment())
            R.id.nav_picture -> replaceFragment(PictureFragment())
            R.id.nav_gps -> replaceFragment(GpsFragment())
            R.id.nav_music -> replaceFragment(MusicFragment())
            R.id.nav_off -> replaceFragment(OffFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .commit()
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

}