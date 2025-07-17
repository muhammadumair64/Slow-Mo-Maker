package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
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
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.ads.AdSize
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentVideoCompressorBinding
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


class VideoCompressorFragment : Fragment() {

    val binding by lazy {
        FragmentVideoCompressorBinding.inflate(layoutInflater)
    }
    private val uris = mutableListOf<Uri>()
    private val viewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private var videoDuration = 0
    val TAG = "CompressorFragmentTag"
    private lateinit var player: ExoPlayer
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadAds()
        initViews()
        return binding.root
    }
    fun initViews(){
        uris.add(File(dataShareViewModel.outputPath).toUri())

        val mediaItem = dataShareViewModel.outputPath.let { MediaItem.fromUri(it) }
        videoPlayer(mediaItem)
        binding.apply {
            if (dataShareViewModel.tabNumber == 5) {
                title.text = "My Videos"
            }

            lifecycleScope.launch {
            fileSize.text  =  formatFileSize(getVideoSize(dataShareViewModel.outputPath,requireActivity().contentResolver))
            }

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
            safeNavigate(R.id.action_videoCompressorFragment_to_dashboardFragment,R.id.videoCompressorFragment)
            }

            compress.setOnClickListener {
                disableMultipleClicking(it,2000)
                processVideo()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.pause()
        player.release()
    }

    override fun onPause() {
        super.onPause()
        player.pause()
        player.release()
    }
    private fun loadAds(){
      //  MyApplication.mInstance.adsManager.showBanner(requireContext(), AdSize.LARGE_BANNER,binding.adView,this.getString(R.string.ADMOB_BANNER_V2),binding.shimmerLayout);
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

    private fun processVideo() {
        if(uris.isNotEmpty()){
            lifecycleScope.launch {
                VideoCompressor.start(
                    context = MyApplication.appContext,
                    uris,
                    isStreamable = false,
                    sharedStorageConfiguration = SharedStorageConfiguration(
                        saveAt = SaveLocation.dcim,
                        subFolderName = "SlowMoMaker"
                    ),
                    configureWith = Configuration(
                        quality = VideoQuality.LOW,
                        videoNames = uris.map { uri -> uri.pathSegments.last() },
                        isMinBitrateCheckEnabled = true,
                    ),
                    listener = object : CompressionListener {
                        override fun onProgress(index: Int, percent: Float) {
                            lifecycleScope.launch(Dispatchers.Main)  {
                                binding.progressText.text  =  "${percent.toInt()}%"
                                binding.compressSeekBar.apply {
                                    progress = percent.toInt()
                                    isClickable = false
                                }
                            }
                        }

                        override fun onStart(index: Int) {
                            try {
                                requireActivity().runOnUiThread {
                                    binding.compress.gone()
                                }
                            }catch (e:Exception){
                                e.localizedMessage
                            }
                        }

                        override fun onSuccess(index: Int, size: Long, path: String?) {
                            try {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    dataShareViewModel.finalFileSize =  formatFileSize(size)
                                    if (path != null) {
                                        dataShareViewModel.outputPath = path
                                        safeNavigate(R.id.action_videoCompressorFragment_to_previewCompressionFragment,R.id.videoCompressorFragment)
                                    }
                                }
                            }catch (e:Exception){
                                e.localizedMessage
                            }

                        }

                        override fun onFailure(index: Int, failureMessage: String) {
                            try {
                                lifecycleScope.launch(Dispatchers.Main)  {
                                    binding.compress.visible()
                                    Toast.makeText(requireContext(), "Already Size is Compressed", Toast.LENGTH_SHORT).show()
                                    Log.wtf("failureMessage", failureMessage)
                                }
                            }catch (e :Exception){
                                e.localizedMessage
                            }
                        }

                        override fun onCancelled(index: Int) {
                            requireActivity().runOnUiThread {
                                binding.compress.visible()
                            }
                        }
                    }
                )
            }
        } else {
            try {
            Toast.makeText(requireContext(), "List Null", Toast.LENGTH_SHORT).show()
            }catch (e:Exception){
                Log.d(TAG, "processVideo: ${e.localizedMessage}")
            }
        }

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
                if (!fromUser) {
                    requireActivity().runOnUiThread {
                        player.seekTo(progress * 1000L)
                    }
                    // Update the player's position
                    Log.d(TAG, "onProgressChanged: ${progress}")
                    if(progress == seekBar.max ){
                        binding.playPauseController.setImageResource(R.drawable.play)
                    }
                    if(player.isPlaying){
                        binding.playPauseController.setImageResource(R.drawable.pause )
                    }
                    binding.timeStampStart.text = DateUtils.formatElapsedTime(progress.toLong())
                }

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
}
