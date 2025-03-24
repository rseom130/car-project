package com.kimsiso.carproject

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

class MusicController(private val context: Context) {
    private var mediaController: MediaController? = null
    private var uiUpdateReceiver: BroadcastReceiver? = null
    private var titleTextView: TextView? = null
    private var artistTextView: TextView? = null
    private var albumImageView: ImageView? = null
    private var playButton: Button? = null
    private var nextButton: Button? = null
    private var prevButton: Button? = null
    private var isReceiverRegistered = false // ✅ 리시버 등록 여부 확인

    init {
        // 알림 리스너 서비스 접근 권한 확인
        if (!isNotificationServiceEnabled(context)) {
//            Log.d("MusicInfo", "🚨 알림 접근 권한 없음. 설정 화면으로 이동.")
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
//            Log.d("MusicInfo", "✅ 알림 접근 권한이 활성화됨.")
        }

        setupMediaController()
        registerReceiver()
    }

    // UI 요소 연결 (🎵 음악 컨트롤 버튼도 포함)
    fun bindViews(
        titleView: TextView,
        artistView: TextView,
        albumView: ImageView,
        playBtn: Button,
        nextBtn: Button,
        prevBtn: Button
    ) {
        titleTextView = titleView
        artistTextView = artistView
        albumImageView = albumView
        playButton = playBtn
        nextButton = nextBtn
        prevButton = prevBtn

        playButton?.setOnClickListener { togglePlayPause() }
        nextButton?.setOnClickListener { skipToNextTrack() }
        prevButton?.setOnClickListener { skipToPreviousTrack() }

        updatePlayButtonText()
    }

    // 🎵 현재 실행 중인 미디어 세션 가져오기
    private fun setupMediaController() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mediaSessionManager =
                context.getSystemService(MediaSessionManager::class.java)
            val mediaSessions = mediaSessionManager.getActiveSessions(
                ComponentName(context, MusicNotificationListener::class.java)
            )

            if (mediaSessions.isNotEmpty()) {
                mediaController = mediaSessions[0] // 첫 번째 미디어 세션 사용
                updatePlayButtonText()
            } else {
//                Log.d("MusicController", "⚠️ 활성화된 미디어 세션이 없음")
            }
        }
    }

    // ▶ 재생 / ⏸ 일시정지 토글 기능
    fun togglePlayPause() {
//        Log.d("App-Test2", playButton.toString())
        mediaController?.let {
            val playbackState = it.playbackState
            if (playbackState != null) {
                when (playbackState.state) {
                    PlaybackState.STATE_PLAYING -> {
                        it.transportControls.pause()
                        playButton?.text = "PLAY"
                    }
                    PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED -> {
                        it.transportControls.play()
                        playButton?.text = "PAUSE"
                    }
                    // else -> Log.d("MusicController", "⚠️ 미디어 상태 확인 불가")
                }
            }
        }
    }

    // ⏭ 다음 곡으로 스킵
    fun skipToNextTrack() {
        mediaController?.transportControls?.skipToNext()
        playButton?.text = "PAUSE"
    }

    // ⏮ 이전 곡으로 스킵
    fun skipToPreviousTrack() {
        mediaController?.transportControls?.skipToPrevious()
        playButton?.text = "PAUSE"
    }

    // 🔄 현재 미디어 상태에 맞게 playButton의 텍스트 업데이트
    private fun updatePlayButtonText() {
        mediaController?.let {
            val playbackState = it.playbackState
//            Log.d("App-Test", PlaybackState.STATE_PLAYING.toString());
//            Log.d("App-Test", playbackState?.state.toString())
//            Log.d("App-Test", playButton.toString())
            playButton?.text = when (playbackState?.state) {
                PlaybackState.STATE_PLAYING -> "PAUSE"
                PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED -> "PLAY"
                else -> "PLAY"
            }
        }
    }

    // 알림 리스너 권한 함수
    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val cn = ComponentName(context, MusicNotificationListener::class.java)
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(cn.flattenToString()) ?: false
    }

    // ✅ UI 업데이트를 위한 BroadcastReceiver
    private fun updateUI(intent: Intent?) {
        // UI 업데이트 로직
        if (intent?.action == "MUSIC_INFO_UPDATE") {
            val title = intent.getStringExtra("title") ?: "제목 없음"
            val artist = intent.getStringExtra("artist") ?: "가수 없음"
            val albumByteArray = intent.getByteArrayExtra("album")
//            Log.d("App-Test", title)

            var albumArt: Bitmap? = null
            if (albumByteArray != null) {
                albumArt = BitmapFactory.decodeByteArray(albumByteArray, 0, albumByteArray.size)
            }
            // ✅ UI 업데이트
            titleTextView?.text = title
            artistTextView?.text = artist
            albumImageView?.setImageBitmap(albumArt ?: ContextCompat.getDrawable(context!!, R.drawable.ic_launcher_background)?.let { drawable ->
                (drawable as android.graphics.drawable.BitmapDrawable).bitmap
            })

        }
    }

    fun registerReceiver() {
        val filter = IntentFilter("MUSIC_INFO_UPDATE")
//        context.registerReceiver(uiUpdateReceiver, filter, Context.RECEIVER_EXPORTED)
        if (uiUpdateReceiver == null) { // ✅ 중복 등록 방지
            uiUpdateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    updateUI(intent)
                }
            }
            val filter = IntentFilter("MUSIC_INFO_UPDATE")
            context.registerReceiver(uiUpdateReceiver, filter, Context.RECEIVER_EXPORTED)
        }
    }

    fun unregisterReceiver() {
//        context.unregisterReceiver(uiUpdateReceiver)
        uiUpdateReceiver?.let {
            context.unregisterReceiver(it)
            uiUpdateReceiver = null // ✅ 해제 후 null 처리
        }
    }
}