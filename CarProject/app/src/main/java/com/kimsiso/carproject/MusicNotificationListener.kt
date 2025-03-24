package com.kimsiso.carproject

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.provider.Settings
import android.widget.Toast
import java.io.ByteArrayOutputStream

class MusicNotificationListener : NotificationListenerService() {
    private var requestReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
//        Log.d("App-Test", "🟢 서비스 시작됨. 기존 알림을 확인 중...")
        checkActiveNotifications() // ✅ 앱 실행 시 즉시 현재 알림 정보 확인

        requestReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
//            Log.d("App-Test", "🔄 기존 알림 정보를 요청받음! 다시 확인 중...")
                checkActiveNotifications() // ✅ 기존 알림 확인 및 재전송
            }
        }

        // 📌 메인에서 요청할 때 기존 알림을 다시 확인하는 브로드캐스트 리시버 등록
        val filter = IntentFilter("REQUEST_MUSIC_INFO")
        registerReceiver(requestReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
//        Log.d("App-Test", "🔄 알림 리스너 연결됨. 현재 활성화된 알림 확인 중...")
        checkActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
//        Log.d("App-Test", "🚨 알림 리스너 서비스가 비활성화됨. 재시작 필요.")
    }


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        checkActiveNotifications()
    }

    // ✅ 중복되지 않은 올바른 `onNotificationRemoved()` 구현
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        if (sbn != null) {
//            Log.d("App-Test", "🛑 음악 알림 제거됨: ${sbn.packageName}")
        } else {
//            Log.d("App-Test", "🛑 알 수 없는 알림이 제거됨")
        }

        // 🔍 현재 활성화된 알림이 남아있는지 확인 후 실행
        if (activeNotifications.isNotEmpty()) {
            checkActiveNotifications()
        } else {
//            Log.d("App-Test", "⚠️ 남아 있는 음악 알림이 없음.")
        }
    }

    private fun checkActiveNotifications() {
        val activeNotifications = activeNotifications // 현재 모든 활성화된 알림 가져오기

        for (sbn in activeNotifications) {
            val packageName = sbn.packageName
            val extras = sbn.notification.extras

            // 🎵 미디어 관련 알림인지 확인
            if (sbn.notification.category == Notification.CATEGORY_TRANSPORT) {
                val artist = extras.getString(Notification.EXTRA_TEXT) ?: "알 수 없음"
                val title = extras.getString(Notification.EXTRA_TITLE) ?: "알 수 없음"

                // 🖼️ 상태바(Notification)에서 큰 앨범 이미지 가져오기
                var albumArt: Bitmap? = null

                // 1️⃣ `MediaSession`에서 고해상도 이미지 가져오기 (더 큰 이미지가 있을 가능성이 높음)
                if (albumArt == null) {
                    albumArt = getMediaSessionAlbumArt(applicationContext)
                    if (albumArt != null) {
//                        Log.d("MusicInfo-Test", "🎨 FLO 미디어 세션에서 고해상도 앨범 이미지 가져옴")
                    }
                }


                // 2️⃣ `Notification.EXTRA_LARGE_ICON_BIG`에서 먼저 시도
                if (albumArt == null) {
                    val albumIcon = extras.getParcelable<Icon>(Notification.EXTRA_LARGE_ICON_BIG)
                        ?: extras.getParcelable<Icon>(Notification.EXTRA_LARGE_ICON)

                    if (albumIcon != null) {
                        albumArt = iconToBitmap(applicationContext, albumIcon)
//                        Log.d("MusicInfo-Test", "🎨 EXTRA_LARGE_ICON_BIG 사용 (${albumArt?.width}x${albumArt?.height})")
                    }
                }


                // ✅ 앨범 이미지 크기 확인
                if (albumArt != null) {
                    val width = albumArt.width
                    val height = albumArt.height
                    // Log.d("MusicInfo-Test", "🖼️ 최종 앨범 이미지 크기: ${width}x${height}")
                }

                Log.d("MusicInfo", "🎵 감지된 음악 앱: $packageName")
                Log.d("MusicInfo", "🎶 제목: $title")
                Log.d("MusicInfo", "🎤 가수: $artist")
                Log.d("MusicInfo", "🖼️ 앨범 이미지: ${if (albumArt != null) "있음" else "없음"}")

                sendMusicInfoToActivity(title, artist, albumArt)
            }
        }
    }

    // 🖼️ Icon을 Bitmap으로 변환하는 함수
    private fun iconToBitmap(context: Context, icon: Icon): Bitmap? {
        return try {
            val drawable = icon.loadDrawable(context) ?: return null
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sendMusicInfoToActivity(songTitle: String, artist: String, albumArt: Bitmap?) {
        val intent = Intent("MUSIC_INFO_UPDATE")
        intent.putExtra("title", songTitle)
        intent.putExtra("artist", artist)

        // 📌 Bitmap을 ByteArray로 변환
        if (albumArt != null) {
            val stream = ByteArrayOutputStream()
            albumArt.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            intent.putExtra("album", byteArray)
        }
//        Log.d("App-Test", "🎵 음악 정보 브로드캐스트 전송됨: $songTitle - $artist")
        sendBroadcast(intent)
    }

    fun getMediaSessionAlbumArt(context: Context): Bitmap? {
        try {
            val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)
            val componentName = ComponentName(context, MusicNotificationListener::class.java) // 📌 정확한 서비스 지정
            val mediaSessions = mediaSessionManager.getActiveSessions(componentName) // 현재 활성화된 미디어 세션 가져오기

            for (session in mediaSessions) {
                val metadata: MediaMetadata? = session.metadata
                if (metadata != null) {
                    val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) // 🖼️ 고해상도 앨범 이미지 가져오기
                        ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART) // 대체 가능 키

                    if (albumArt != null) {
                        // Log.d("MusicInfo-Get", "🎨 고해상도 앨범 이미지 가져옴 (${albumArt.width}x${albumArt.height})")
                        return albumArt
                    }
                }
            }
        } catch (e: SecurityException) {
            // Log.d("MusicInfo", "🚨 MEDIA_CONTENT_CONTROL 권한이 없음! 설정에서 알림 접근을 허용하세요.", e)
            requestNotificationAccess(context) // 📌 설정 화면 열기
        }
        // Log.d("MusicInfo-Get", "❌ FLO 미디어 세션에서 고해상도 앨범 이미지를 찾을 수 없음")
        return null
    }

    fun requestNotificationAccess(context: Context) {
        Toast.makeText(context, "알림 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ BroadcastReceiver가 등록되어 있으면 해제
        requestReceiver?.let {
            unregisterReceiver(it)
            requestReceiver = null
        }
    }
}