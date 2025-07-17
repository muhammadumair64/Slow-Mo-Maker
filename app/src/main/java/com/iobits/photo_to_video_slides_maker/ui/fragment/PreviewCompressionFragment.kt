package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.applovin.sdk.AppLovinSdkUtils
import com.google.android.gms.ads.AdSize
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentPreviewCompressionBinding
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.utils.disableMultipleClicking
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.utils.visible
import com.iobits.photo_to_video_slides_maker.videoCompressor.CompressionListener
import com.iobits.photo_to_video_slides_maker.videoCompressor.VideoCompressor
import com.iobits.photo_to_video_slides_maker.videoCompressor.VideoQuality
import com.iobits.photo_to_video_slides_maker.videoCompressor.config.Configuration
import com.iobits.photo_to_video_slides_maker.videoCompressor.config.SaveLocation
import com.iobits.photo_to_video_slides_maker.videoCompressor.config.SharedStorageConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class PreviewCompressionFragment : Fragment() {
    val binding by lazy {
        FragmentPreviewCompressionBinding.inflate(layoutInflater)
    }
    private val viewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private var videoDuration = 0
    val TAG = "PreviewFragmentTag"
    private lateinit var player: ExoPlayer
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadAds()
        initViews()
        // Inflate the layout for this fragment
        return binding.root
    }
    private fun loadAds() {
        MyApplication.mInstance.adsManager.loadNativeAd(
            requireActivity(),
            binding.adView,
            com.iobits.photo_to_video_slides_maker.managers.AdsManager.NativeAdType.NOMEDIA_MEDIUM,
            getString(R.string.ADMOB_NATIVE_WITH_MEDIA_V2),
            binding.shimmerLayout
        )
    }

    fun initViews(){
        val mediaItem = dataShareViewModel.outputPath.let { MediaItem.fromUri(it) }
        videoPlayer(mediaItem)
        binding.apply {
            if (dataShareViewModel.tabNumber == 5) {
                title.text = "My Videos"
            }
            lifecycleScope.launch {
                fileSize.text  =  formatFileSize(getVideoSize(dataShareViewModel.outputPath,requireActivity().contentResolver))
            }
            binding.fileSize.text = dataShareViewModel.finalFileSize
            playPauseController.setOnClickListener {
                disableMultipleClicking(it)
                if (player.isPlaying) {
                    player.pause()
                    playPauseController.setImageResource(R.drawable.play)
                } else {
                    player.release()
                    videoPlayer(mediaItem)
                    playPauseController.setImageResource(R.drawable.pause)
                }
            }

            backBtn.setOnClickListener {
                player.release()
             safeNavigate(R.id.action_previewCompressionFragment_to_dashboardFragment,R.id.previewCompressionFragment)
            }
            compress.setOnClickListener {
                player.release()
                safeNavigate(R.id.action_previewCompressionFragment_to_dashboardFragment,R.id.previewCompressionFragment)
            }

        }
        lifecycleScope.launch(Dispatchers.IO) {
            delay(2000)
            withContext(Dispatchers.Main){
                try {
                    viewModel.apply {
                        Log.d(TAG, "before media store")
                        updateMediaStore(
                            requireContext(),
                            dataShareViewModel.outputPath,
                            getFileNameFromPath(dataShareViewModel.outputPath),
                            completeListener,
                            progressListener
                        )
                    }
                }catch (e:Exception){
                    e.localizedMessage
                }

            }
        }
    }
    fun getFileNameFromPath(filePath: String): String {
        return File(filePath).name
    }
    private fun getVideoSize(filePath: String, contentResolver: ContentResolver): Long {
        var size: Long = 0

        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Video.Media.DATA + "=?"
        val selectionArgs = arrayOf(filePath)

        val projection = arrayOf(MediaStore.Video.Media.SIZE)
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)

            cursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                    size = cursor.getLong(sizeIndex)
                } else {
                    Log.e("VideoSize", "Cursor is empty")
                }
            } ?: Log.e("VideoSize", "Cursor is null")
        } catch (e: Exception) {
            Log.e("VideoSize", "Error: ${e.message}")
        } finally {
            cursor?.close()
        }

        Log.d("VideoSize", "Video size: $size")

        return size
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var fileSize = sizeInBytes.toDouble()
        var unitIndex = 0

        while (fileSize > 1024 && unitIndex < units.size - 1) {
            fileSize /= 1024
            unitIndex++
        }
        return String.format("%.2f %s", fileSize, units[unitIndex])
    }
    private fun videoPlayer(mediaItem: MediaItem?) {
        player = ExoPlayer.Builder(requireContext()).build()
        binding.ExoPlayer.player = player
        if (mediaItem != null) {
            player.setMediaItem(mediaItem)
        }

        // Create a PlaybackParameters object to set the speed to 2x for the specified duration
        val playbackParameters = PlaybackParameters(1f, 1f)
        // Set the PlaybackParameters to the player
        player.playbackParameters = playbackParameters
        val startPositionUs = 1f
        player.prepare()
        player.play()
        setController()
    }

    private fun setController()
    {
        if(dataShareViewModel.outputPath != ""){
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(dataShareViewModel.outputPath)
            mediaPlayer.prepare()
            videoDuration = mediaPlayer.duration
            mediaPlayer.release()
            Log.d(TAG, "setController: $videoDuration")
            binding.apply {
                timeStampEnd.text = DateUtils.formatElapsedTime(videoDuration/1000.toLong())
            }
            seekbar(videoDuration)
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
                        player.seekTo(progress * 1000L)
                    }
                    // Update the player's position
                    Log.d(TAG, "onProgressChanged: ${progress}")
                }
                if(progress == seekBar.max ){
                    binding.playPauseController.setImageResource(R.drawable.play)
                }
                if(player.isPlaying){
                    binding.playPauseController.setImageResource(R.drawable.pause )
                }
                binding.timeStampStart.text = DateUtils.formatElapsedTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                // Update the seek bar position
                seekBar.progress = (player.currentPosition / 1000).toInt()

                // Schedule the next update
                handler.postDelayed(this, 0)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }


}
