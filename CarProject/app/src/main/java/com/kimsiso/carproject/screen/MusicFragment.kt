package com.kimsiso.carproject.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kimsiso.carproject.MusicController
import com.kimsiso.carproject.R

class MusicFragment : Fragment() {
    // MusicController 클래스 선언
    private lateinit var musicController: MusicController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_music, container, false)
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

    }


    override fun onResume() {
        super.onResume()

        // MusicController 리시버 등록
        musicController.registerReceiver()
        val intent = Intent("REQUEST_MUSIC_INFO")
        requireContext().sendBroadcast(intent)
    }

    override fun onPause() {
        super.onPause()

        // musicController 리시버 종료
        if (::musicController.isInitialized) {
            musicController.unregisterReceiver()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // musicController 리시버 종료
        if (::musicController.isInitialized) {
            musicController.unregisterReceiver()
        }
    }
}