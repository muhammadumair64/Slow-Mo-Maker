package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
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
import com.applovin.sdk.AppLovinSdkUtils.runOnUiThread
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentTrimmerBinding
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

class TrimmerFragment : Fragment() {

    val TAG = "TrimmerFragmentTAG"
    val binding by lazy {
        FragmentTrimmerBinding.inflate(layoutInflater)
    }

    private var miniDistance = 0
    private val mainViewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        try {
            initViews()
            initListeners()
        }catch (e:Exception){
            Log.d(TAG, "onCreateView: ERROR ${e.localizedMessage}")
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        dataShareViewModel.fullScreenCallBack = {
            try {
                initViews()
                initListeners()
            } catch (e: Exception) {
                e.localizedMessage
            }
        }
    }

    private fun initListeners(){
        binding.apply {
            check.setOnClickListener {
                if(dataShareViewModel.trimEndingPoint != 0){
                    if( EditingOptionsValidator.editorOptions.contains(Constants.trim)){
                        EditingOptionsValidator.editorOptions.remove(Constants.trim)
                        EditingOptionsValidator.editorOptions.add(Constants.trim)
                    }else{
                        EditingOptionsValidator.editorOptions.add(Constants.trim)
                    }
                    safeNavigate(R.id.action_trimmerFragment_to_videoEditorTabsFragment,R.id.trimmerFragment)
                    dataShareViewModel.onEditOptionDoneClick?.invoke(2)
                }
            }
            cross.setOnClickListener {
                safeNavigate(R.id.action_trimmerFragment_to_videoEditorTabsFragment,R.id.trimmerFragment)
                dataShareViewModel.apply {
                    trimStartingPoint=0
                    trimEndingPoint =0
                }
            }
        }
    }
    fun initViews() {

        if (dataShareViewModel.soloEditor == Constants.trim) {
            binding.apply {
                cross.invisible()
                check.invisible()
                EditingOptionsValidator.editorOptions.add(Constants.trim)
                EditingOptionsValidator.isUsingOnlyTrimmer = true
            }
        }

        try {
            dataShareViewModel.mVideoItem?.let {
                mainViewModel.framesFromVideo(
                    requireContext(),
                    dataShareViewModel.frames,
                    dataShareViewModel.mVideoItem!!.artUri,
                    binding.videoFrameView
                )
            }
        }catch (e:Exception){
            Log.d(TAG, "initViews: ERROR ${e.localizedMessage}")}

        mainViewModel.frameCallback = {
            seekbar(it)
            frameRangeSlider(it)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun seekbar(videoDuration: Int) {
        val seekBar = binding.frameSeekBar
//        seekBar.progressDrawable = null
//        seekBar.splitTrack = false
        Log.d(TAG, "Video duration is = $videoDuration")
        seekBar.max = videoDuration / 1000

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    runOnUiThread {
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
                    dataShareViewModel.trimEndingPoint = videoDuration/2
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
                                trimStartingPoint = currentThumbPositions[0].toInt()
                                trimEndingPoint = currentThumbPositions[1].toInt()
                                val timeStart = currentThumbPositions[0] / 1000
                                val timeEnd = currentThumbPositions[1] / 1000
                                binding.timeStampStart.text = DateUtils.formatElapsedTime(timeStart.toLong())
                                binding.timeStampEnd.text = DateUtils.formatElapsedTime(timeEnd.toLong())
                                Log.d(
                                    TAG,
                                    "thumb values is ${currentThumbPositions[0]} ============== ${currentThumbPositions[1]}  ======= ${trimStartingPoint}  ===== $trimEndingPoint"
                                )
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
    private fun setRangeSliderWidth() {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val desiredWidth = screenWidth * 0.5
        val rangeSlider = binding.rectangle
        val layoutParams = rangeSlider.layoutParams
        layoutParams.width = desiredWidth.toInt()
        rangeSlider.layoutParams = layoutParams
    }
}
