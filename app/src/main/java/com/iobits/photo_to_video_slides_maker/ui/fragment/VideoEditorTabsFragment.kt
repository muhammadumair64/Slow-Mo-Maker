package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.applovin.sdk.AppLovinSdkUtils
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentVideoEditorTabsBinding
import com.iobits.photo_to_video_slides_maker.managers.AnalyticsManager
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.Constants
import com.iobits.photo_to_video_slides_maker.utils.EditingOptionsValidator
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class VideoEditorTabsFragment : Fragment() {
    val TAG = "VideoEditorTabsFragmentTag"
    val binding by lazy {
        FragmentVideoEditorTabsBinding.inflate(layoutInflater)
    }
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initEditorLayout()
        initListeners()
        return binding.root
    }
    private fun initListeners() {
        binding.apply {
            trim.setOnClickListener {
             //   dataShareViewModel.hideExport?.invoke(true)
                AnalyticsManager.logEvent("CLICK_ON_EDITOR_TRIM",null)
                safeNavigate(
                    R.id.action_videoEditorTabsFragment_to_trimmerFragment,
                    R.id.videoEditorTabsFragment
                )
            }
            speed.setOnClickListener {
             //   dataShareViewModel.hideExport?.invoke(true)
                AnalyticsManager.logEvent("CLICK_ON_SPEED",null)
                safeNavigate(R.id.action_videoEditorTabsFragment_to_speedFragment, R.id.videoEditorTabsFragment)
            }
            music.setOnClickListener {
                AnalyticsManager.logEvent("CLICK_ON_MUSIC",null)
             dataShareViewModel.onMusicTabClick?.invoke()
            }
            if(EditingOptionsValidator.editorOptions.contains(Constants.trim)){
                trimmerTimeStamp.visible()
                trimStartTime.text = DateUtils.formatElapsedTime((dataShareViewModel.trimStartingPoint / 1000).toLong())
                trimEndTime.text = DateUtils.formatElapsedTime((dataShareViewModel.trimEndingPoint / 1000).toLong())
            }
        }
        try {
            mainViewModel.framesFromVideo (
                requireContext(),
                dataShareViewModel.frames,
                dataShareViewModel.mVideoItem!!.artUri,
                binding.videoFrameView
            )
        }catch (e:Exception){
            e.localizedMessage
        }

        mainViewModel.frameCallback = {
            seekbar(it)
        }
    }
    private fun initEditorLayout() {
        if (dataShareViewModel.soloEditor != "") {
            when (dataShareViewModel.soloEditor) {
                Constants.trim -> { safeNavigate(
                    R.id.action_videoEditorTabsFragment_to_trimmerFragment,
                    R.id.videoEditorTabsFragment
                )}
                Constants.slowMo -> { safeNavigate(
                    R.id.action_videoEditorTabsFragment_to_speedFragment,
                    R.id.videoEditorTabsFragment
                )}
                Constants.reverse -> {
                    safeNavigate(
                        R.id.action_videoEditorTabsFragment_to_reverseFragment,
                        R.id.videoEditorTabsFragment
                    )
                }
            }
        }
    }
    private fun seekbar(videoDuration: Int) {
        val seekBar = binding.frameSeekBar
//        seekBar.progressDrawable = null
//        seekBar.splitTrack = false
        Log.d(TAG, "Video duration is = $videoDuration")
        seekBar.max = videoDuration / 1000

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    AppLovinSdkUtils.runOnUiThread {
                        dataShareViewModel.player?.seekTo(progress * 1000L)
                    }// Update the player's position
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                try {
                    // Update the seek bar position
                    seekBar.progress = (dataShareViewModel.player!!.currentPosition / 1000).toInt()
                    binding.timeStampStart.text = DateUtils.formatElapsedTime(dataShareViewModel.player!!.currentPosition/1000)
                    binding.timeStampEnd.text = DateUtils.formatElapsedTime((mainViewModel.duration/1000).toLong())
                } catch (e: Exception) {
                    e.localizedMessage
                }
                // Schedule the next update
                handler.postDelayed(this, 0)
            }
        })
        exoListener(seekBar)
    }
    private fun exoListener(seekBar: SeekBar) {
        dataShareViewModel.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                while (true) {
                    delay(700)
                    withContext(Dispatchers.Main) {
                        val currentPosition = player?.currentPosition // current position of the video in milliseconds
                        if (currentPosition != null) {
                            if (currentPosition < trimStartingPoint && trimStartingPoint != 0) {
                                player?.seekTo(trimStartingPoint.toLong())
                                seekBar.progress = (trimStartingPoint / 1000).toInt()
                            } else if (currentPosition > trimEndingPoint && trimEndingPoint != 0) {
                                player?.seekTo(trimStartingPoint.toLong())
                                seekBar.progress = (trimStartingPoint / 1000).toInt()
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        dataShareViewModel.apply {
            hideExport?.invoke(false)
            trimStartingPoint =0
            trimEndingPoint = 0
            speedEndingPoint =0
            speedStartingPoint = 0
            mainViewModel.speedUnit = 1f
        }
    }
}
