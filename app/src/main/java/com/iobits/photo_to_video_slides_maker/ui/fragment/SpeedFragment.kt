package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackParameters
import com.applovin.sdk.AppLovinSdkUtils
import com.applovin.sdk.AppLovinSdkUtils.runOnUiThread
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentSpeedBinding
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.Constants
import com.iobits.photo_to_video_slides_maker.utils.EditingOptionsValidator
import com.iobits.photo_to_video_slides_maker.utils.invisible
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeedFragment : Fragment() {
    val TAG = "SpeedFragmentTag"
    val binding by lazy {
        FragmentSpeedBinding.inflate(layoutInflater)
    }

    private var miniDistance = 0
    private val mainViewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initViews()
        initListeners()

        return binding.root
    }

    fun initViews() {
        try {
            if (dataShareViewModel.soloEditor == Constants.slowMo) {
                binding.apply {
                    cross.invisible()
                    check.invisible()
                    EditingOptionsValidator.editorOptions.add(Constants.slowMo)
                    EditingOptionsValidator.isUsingOnlySpeed = true
                }
            }
            if(dataShareViewModel.mVideoItem != null){
                mainViewModel.framesFromVideo(
                    requireContext(),
                    dataShareViewModel.frames,
                    dataShareViewModel.mVideoItem!!.artUri,
                    binding.videoFrameView
                )
            }

            mainViewModel.frameCallback = {
                seekbar(it)
                speedSeekbar(it)
                frameRangeSlider(it)
            }
        }catch (e :Exception){
            e.localizedMessage
        }
    }
    private fun initListeners(){
        binding.apply {
            check.setOnClickListener {
                if(mainViewModel.speedUnit != 1f){
                    if (EditingOptionsValidator.editorOptions.contains(Constants.slowMo)){
                        EditingOptionsValidator.editorOptions.remove(Constants.slowMo)
                        EditingOptionsValidator.editorOptions.add(Constants.slowMo)
                    }else{
                        EditingOptionsValidator.editorOptions.add(Constants.slowMo)
                    }
                    dataShareViewModel.onEditOptionDoneClick?.invoke(1)
                    safeNavigate(R.id.action_speedFragment_to_videoEditorTabsFragment,R.id.speedFragment)
                }
            }
            cross.setOnClickListener {
                safeNavigate(R.id.action_speedFragment_to_videoEditorTabsFragment,R.id.speedFragment)
                dataShareViewModel.apply {
                    trimStartingPoint=0
                    trimEndingPoint =0
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        dataShareViewModel.fullScreenCallBack = {
            try {
                initViews()
            } catch (e: Exception) {
                e.localizedMessage
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun seekbar(videoDuration: Int) {
        val seekBar = binding.frameSeekBar
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
                } catch (e: Exception) {
                    e.localizedMessage
                }
                // Schedule the next update
                handler.postDelayed(this, 0)
            }
        })
        exoListener()
    }

    private fun exoListener() {
        dataShareViewModel.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                while (true) {
                    delay(700)
                    withContext(Dispatchers.Main) {
                        val currentPosition =
                            player?.currentPosition // current position of the video in milliseconds
                        if (currentPosition != null) {
                            if (currentPosition in speedStartingPoint..speedEndingPoint) {
                                changeVideoSpeedPreview(mainViewModel.speedUnit)
                            } else {
                                val playbackParameters = PlaybackParameters(1f, 1f)
                                dataShareViewModel.player?.playbackParameters = playbackParameters
                            }
                        }
                    }
                }
            }
        }
    }

    /** this slider is a Range bar and control white window on the frames */
    private fun frameRangeSlider(videoDuration: Int) {
        miniDistance = (videoDuration / 100) * 20
        Log.d(TAG, "frameRangeSlider: ${videoDuration}")
        try {
            binding.apply {
                frameSlider.apply {
                    Log.d(TAG, "IN SLIDER")
                    trackActiveTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    trackInactiveTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    setCustomThumbDrawable(R.drawable.seek_bar_progress_tp)

                    isFocusable = false
                    trackHeight = 10
                    valueFrom = 0F
                    valueTo = videoDuration.toFloat()

                    val startValue = 0
                    val endValue = videoDuration / 2
                    val values = mutableListOf(startValue.toFloat(), endValue.toFloat())
                    frameSlider.values = values
                    binding.timeStampEnd.text =
                        DateUtils.formatElapsedTime(endValue / 1000.toLong())
                    // Define a variable to keep track of the previous thumb positions
                    var previousThumbPositions = frameSlider.values
                    setRangeSliderWidth()
                    Log.d(TAG, "IN SLIDER BEFORE LISTENER")

                    frameSlider.addOnChangeListener { slider, _, _ ->
                        try {
                            // Get the current thumb positions
                            val currentThumbPositions = slider.values
                            dataShareViewModel.apply {
                                speedStartingPoint = currentThumbPositions[0].toInt()
                                speedEndingPoint = currentThumbPositions[1].toInt()
                                val timeStart = currentThumbPositions[0] / 1000
                                val timeEnd = currentThumbPositions[1] / 1000
                                binding.timeStampStart.text =
                                    DateUtils.formatElapsedTime(timeStart.toLong())
                                binding.timeStampEnd.text =
                                    DateUtils.formatElapsedTime(timeEnd.toLong())
                                Log.d(TAG, "thumb values is ${currentThumbPositions[0]} ============== ${currentThumbPositions[1]}  ======= ${speedStartingPoint}  ===== $speedEndingPoint")
                            }
                            // Check if the minimum distance is met
                            if (currentThumbPositions[1] - currentThumbPositions[0] < miniDistance) {
                                // If not, update the values to meet the minimum distance
                                val newValue = currentThumbPositions[0] + miniDistance
                                if (newValue > videoDuration) {
                                    slider.setValues(
                                        (videoDuration - miniDistance).toFloat(),
                                        videoDuration.toFloat()
                                    )
                                } else {
                                    slider.setValues(currentThumbPositions[0], newValue)
                                }
                                // Update the current thumb positions with the new values
                                previousThumbPositions = slider.values

                            } else {
                                // If the minimum distance is met, update the previous thumb positions
                                previousThumbPositions = currentThumbPositions
                            }
                            // Calculate the x position and width of the rectangle
                            val thumb1X =
                                slider.left + (previousThumbPositions[0] - slider.valueFrom) / (slider.valueTo - slider.valueFrom) * slider.width
                            val thumb2X =
                                slider.left + (previousThumbPositions[1] - slider.valueFrom) / (slider.valueTo - slider.valueFrom) * slider.width
                            val rectangleX = thumb1X + slider.thumbRadius
                            val rectangleWidth = thumb2X - thumb1X - 2 * slider.thumbRadius

                            // Set the position and width of the rectangle
                            rectangle.x = rectangleX
                            Log.d(
                                TAG,
                                "rectangle width is ======= $rectangleWidth ======== $rectangleX"
                            )
                            rectangle.layoutParams.width = rectangleWidth.toInt()
                            rectangle.requestLayout()

                        } catch (e: Exception) {
                            Log.d(TAG, "Error: ${e.localizedMessage}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "ERROR  : ${e.localizedMessage}")
        }
    }

    @SuppressLint("SuspiciousIndentation", "ClickableViewAccessibility")
    fun speedSeekbar(videoDuration: Int) {
        val seekBar = binding.speedRangeLayout.speedSeekBar
        seekBar.progressDrawable = null
        seekBar.splitTrack = false
        Log.d(TAG, "Video duration is = $videoDuration")
        seekBar.max = 80
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.min = 20
        }
        seekBar.progress = 50

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    runOnUiThread {
//                        when (progress) {
//                            in 20..25 -> {
//                                seekBar.progress = 20
//                                showPurchase()
//                            }
//
//                            in 75..80 -> {
//                                seekBar.progress = 80
//                                showPurchase()
//                            }
//                        }
                        dataShareViewModel.speedBarProgress = progress
                        mainViewModel.speedCalculator(progress)
                        Log.d(TAG, "speed progress is === $progress")
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun changeVideoSpeedPreview(speed: Float) {
        val playbackParameters = PlaybackParameters(speed, 1f)
        dataShareViewModel.player?.playbackParameters = playbackParameters
    }

    private fun setRangeSliderWidth() {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val desiredWidth = screenWidth * 0.5
        val rangeSlider = binding.rectangle
        val layoutParams = rangeSlider.layoutParams
        layoutParams.width = desiredWidth.toInt()
        rangeSlider.layoutParams = layoutParams
    }
}
