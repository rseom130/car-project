package com.kimsiso.carproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class SystemManager(private val context: Context) {
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var batteryTextView: TextView
    private lateinit var refreshButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private var batteryReceiver: BroadcastReceiver? = null

    // UI 요소 바인딩
    fun bindViews(dateTextView: TextView, timeTextView: TextView, batteryTextView: TextView, refreshButton: Button) {
        this.dateTextView = dateTextView
        this.timeTextView = timeTextView
        this.batteryTextView = batteryTextView
        this.refreshButton = refreshButton

        startUpdatingTime()
        registerBatteryReceiver()

        // 🔄 화면 새로고침 버튼 설정
        refreshButton.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            (context as? MainActivity)?.finish() // 현재 액티비티 종료
            context.startActivity(intent) // 새로운 액티비티 시작
        }
    }

    // ✅ 날짜 및 시간 업데이트
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            dateTextView.text = dateFormat.format(now)
            timeTextView.text = timeFormat.format(now)
            handler.postDelayed(this, 500) // 0.5초마다 업데이트
        }
    }

    fun startUpdatingTime() {
        handler.postDelayed(updateTimeRunnable, 500)
    }

    fun stopUpdatingTime() {
        handler.removeCallbacks(updateTimeRunnable)
    }

    // ✅ 배터리 상태 감지
    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
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
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun unregisterBatteryReceiver() {
        batteryReceiver?.let {
            context.unregisterReceiver(it)
            batteryReceiver = null
        }
    }

    // ✅ 정리 함수
    fun destroy() {
        stopUpdatingTime()
        unregisterBatteryReceiver()
    }
}