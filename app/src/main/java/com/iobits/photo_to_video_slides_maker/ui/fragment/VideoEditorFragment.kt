package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.google.android.gms.ads.AdSize
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentVideoEditorBinding
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.dataModels.VideoEditParams
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.Constants
import com.iobits.photo_to_video_slides_maker.utils.EditingOptionsValidator
import com.iobits.photo_to_video_slides_maker.utils.handleBackPress
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.services.VideoEditorProgressService
import com.iobits.photo_to_video_slides_maker.ui.activities.ResultActivity
import com.iobits.photo_to_video_slides_maker.ui.dataModels.MusicEditParams
import com.iobits.photo_to_video_slides_maker.utils.AdsCounter
import com.iobits.photo_to_video_slides_maker.utils.disableMultipleClicking
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.showExitDialogue
import com.iobits.photo_to_video_slides_maker.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(UnstableApi::class)
class VideoEditorFragment : Fragment() {
    val TAG = "VideoEditorFragmentTag"
    val binding by lazy {
        FragmentVideoEditorBinding.inflate(layoutInflater)
    }
    var name = ""
    var output = ""

    var dX = 0f
    var dY = 0f
    private var finalOverlayX = 0f
    private var finalOverlayY = 0f
    val textContent = "MY SAMPLE TEXT"

    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var isFullScreen = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initViews()
        loadAds()
        initListeners()
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun loadAds() {
        MyApplication.mInstance.adsManager.showBanner(
            requireContext(),
            AdSize.BANNER,
            binding.adView,
            this.getString(R.string.ADMOB_BANNER_V2),
            binding.shimmerLayout
        );
    }

    fun initViews() {
        dataShareViewModel.editType = Constants.video
        try {
            val mediaItem = dataShareViewModel.mVideoItem!!.artUri.let { MediaItem.fromUri(it) }
            videoPlayer(mediaItem)
        } catch (e: Exception) {
            e.localizedMessage
        }
        binding.apply {
//            overlayText.setOnTouchListener { view, event ->
//                when (event.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        dX = view.x - event.rawX
//                        dY = view.y - event.rawY
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        view.animate()
//                            .x(event.rawX + dX)
//                            .y(event.rawY + dY)
//                            .setDuration(0)
//                            .start()
//                    }
//
//                    MotionEvent.ACTION_UP -> {
//                        // Save the final position only once after adjustment
//                        finalOverlayX = view.x
//                        finalOverlayY = view.y
//                    }
//                }
//                true
//            }
            binding.ExoPlayer.setOverlayText("MY SAQMPLE RHLksahd")

            playPause.setOnClickListener {
                dataShareViewModel.apply {
                    if (player?.isPlaying == true) {
                        player?.pause()
                        binding.playPause.setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.play
                            )
                        )
                    } else {
                        player?.play()
                        binding.playPause.setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.pause
                            )
                        )
                    }
                }
            }
            fullScreen.setOnClickListener {
                isFullScreen = if (isFullScreen) {
                    guideline3.setGuidelinePercent(0.75f)
                    dataShareViewModel.fullScreenCallBack?.invoke()
                    false
                } else {
                    guideline3.setGuidelinePercent(1f)
                    true
                }
            }
            backBtn.setOnClickListener {
                moveBack()
            }
        }
    }

    private fun initListeners() {
        dataShareViewModel.hideExport = {
            if (it) {
                binding.export.gone()
            } else {
                binding.export.visible()
            }
        }
        dataShareViewModel.onEditOptionDoneClick = { option ->
            if (AdsCounter.showAd()) {
                when (option) {
                    1 -> {
                        commandSetter()
                    }

                    2 -> {
                        commandSetter()

                    }

                    3 -> {
                        //todo
                    }
                }
            } else {
                when (option) {
                    1 -> {

                        commandSetter()
                    }

                    2 -> {
                        commandSetter()
                    }

                    3 -> {
                        //todo
                    }
                }
            }
        }

        dataShareViewModel.onMusicTabClick = {
            safeNavigate(R.id.action_videoEditorFragment_to_audioFragment, R.id.videoEditorFragment)
        }
        binding.export.setOnClickListener {
            disableMultipleClicking(it)


            if (EditingOptionsValidator.isUsingOnlySpeed || EditingOptionsValidator.isUsingOnlyTrimmer || EditingOptionsValidator.isUsingOnlyReverse) {
                dataShareViewModel.isChangeApplied = true
            }
            if (dataShareViewModel.isChangeApplied) {
                if (EditingOptionsValidator.isUsingOnlySpeed || EditingOptionsValidator.isUsingOnlyTrimmer || EditingOptionsValidator.isUsingOnlyReverse) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        commandSetter()
                    }
                } else {
                    outputPathGenerator()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please Apply Some Changes or Click on Tick",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dataShareViewModel.clickOnReverse = {
            lifecycleScope.launch(Dispatchers.IO) {
                dataShareViewModel.isChangeApplied = true
                commandSetter()
            }

        }
    }

    //------------------------------------ LifeCycle methods -------------------------------------//
    override fun onDestroy() {
        super.onDestroy()
        if (dataShareViewModel.player?.isPlaying == true) {
            dataShareViewModel.player?.release()
        }
    }

    override fun onPause() {
        super.onPause()
        dataShareViewModel.player?.pause()
    }

    override fun onResume() {
        super.onResume()
        handleBackPress {
            moveBack()
        }
        if (dataShareViewModel.player?.isPlaying == false) {
            dataShareViewModel.player?.play()
        }
    }

    private fun moveBack() {
        showExitDialogue(requireActivity()) {
            safeNavigate(
                R.id.action_videoEditorFragment_to_dashboardFragment,
                R.id.videoEditorFragment
            )
        }
    }

    //-------------------------------------- Video Player ----------------------------------------//
    private fun videoPlayer(mediaItem: MediaItem?) {
        try {
            dataShareViewModel.apply {

                player = ExoPlayer.Builder(requireContext()).build()
                binding.ExoPlayer.player = player

                if (mediaItem != null) {
                    player?.setMediaItem(mediaItem)
                }

                // Create a PlaybackParameters object to set the speed to 2x for the specified duration
                val playbackParameters = PlaybackParameters(1f, 1f)

                // Set the PlaybackParameters to the player
                player?.apply {
                    this.playbackParameters = playbackParameters
                    prepare()
                    repeatMode = Player.REPEAT_MODE_ALL
                    play()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "videoPlayer: ${e.localizedMessage}")
        }
    }

    //------------------------------------ Output Exporter -----------------------------------------//
    private fun executor() {
        /** start service here */
        dataShareViewModel.isFromSlideShow = false
        try {
            val intent = VideoEditorProgressService.getStartIntent(
                requireContext(),
                dataShareViewModel.params!!
            )
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
//                requireContext().startService(intent)
//            } else {
//                requireContext().startService(intent)
//              //  requireContext().startForegroundService(intent)
//            }
            requireContext().startService(intent)
            Log.d(TAG, "Move on exporter screen")
            safeNavigate(
                R.id.action_videoEditorFragment_to_exportFragment,
                R.id.videoEditorFragment
            )
        } catch (e: Exception) {
            Log.d(TAG, "Executor: ${e.localizedMessage}")
        }

    }

    private fun commandSetter() {
        lifecycleScope.launch {
            try {
                var command = ""
                var input: Uri? = null
                var startTime = 0
                var endTime = mainViewModel.duration
                var trimStartTime = dataShareViewModel.trimStartingPoint
                var trimEndTime = dataShareViewModel.trimEndingPoint
                val speedUnit = mainViewModel.speedForFFmpeg
                val audioPitch = 1 / speedUnit
                Log.d(TAG, "commandSetter: ${EditingOptionsValidator.editorOptions.size}")
                if (EditingOptionsValidator.editorOptions.size == 1) {
                    dataShareViewModel.outputPathGenerator(requireActivity())
                    input = dataShareViewModel.mVideoItem?.artUri
                    Log.d(TAG, "commandSetter: INPUT $input")
                } else {
                    input = Uri.parse("file://${dataShareViewModel.outputPath}")
                    Log.d(TAG, "commandSetter: INPUT $input")
                    dataShareViewModel.outputPathGenerator(requireActivity())
                }

                if (EditingOptionsValidator.editorOptions.contains(Constants.slowMo)) {
                    startTime = dataShareViewModel.speedStartingPoint
                    endTime = dataShareViewModel.speedEndingPoint
                }
                if (EditingOptionsValidator.editorOptions.contains(Constants.trim)) {
                    trimStartTime = dataShareViewModel.trimStartingPoint
                    trimEndTime = dataShareViewModel.trimEndingPoint
                }
                if (!EditingOptionsValidator.editorOptions.contains(Constants.trim)) {
                    dataShareViewModel.params = VideoEditParams(
                        dataShareViewModel.name,
                        input!!,
                        dataShareViewModel.outputPath,
                        trimStartTime,
                        trimEndTime,
                        mainViewModel.duration
                    )
                }
                if (EditingOptionsValidator.isUsingOnlyReverse) {
                    dataShareViewModel.params = VideoEditParams(
                        dataShareViewModel.name,
                        input!!,
                        dataShareViewModel.outputPath,
                        trimStartTime,
                        trimEndTime,
                        mainViewModel.duration
                    )
                    EditingOptionsValidator.editorOptions.add(Constants.reverse)
                    val fInput = FFmpegKitConfig.getSafParameterForRead(
                        requireContext(),
                        input
                    )
                    if (dataShareViewModel.isWithOutAudio) {
                        EditingOptionsValidator.commandMap[Constants.reverse] =
                            ("-y -i $fInput -vf reverse -c:v mpeg4 -b:v 8192k -preset superfast ${dataShareViewModel.outputPath}")
                        //  ("-y -i $fInput -vf reverse -c:v mpeg4 -b:v 2097k -c:a aac -b:a 48k -preset superfast ${dataShareViewModel.outputPath}")
                    } else {
                        EditingOptionsValidator.commandMap[Constants.reverse] =
                            ("-y -i $fInput -vf reverse -af areverse -c:v mpeg4 -b:v 8192k -preset superfast ${dataShareViewModel.outputPath}")
                        // ("-y -i $fInput -vf reverse -af areverse  -c:v mpeg4 -b:v 2097k -c:a aac -b:a 48k -preset superfast ${dataShareViewModel.outputPath}")
                        // ("-y -i $fInput -vf reverse -af areverse  -c:v mpeg4 -b:v 8192k -vcodec mpeg4 -preset superfast ${dataShareViewModel.outputPath}")
                    }
                    dataShareViewModel.isChangeApplied = true
                    Log.d(TAG, "commandSetter: REVERSE VIDEO COMMAND SET")
                    executor()
                }
                if (EditingOptionsValidator.isUsingOnlyTrimmer || EditingOptionsValidator.isUsingOnlySpeed) {
                    dataShareViewModel.params = VideoEditParams(
                        dataShareViewModel.name,
                        input!!,
                        dataShareViewModel.outputPath,
                        trimStartTime,
                        trimEndTime,
                        mainViewModel.duration
                    )
                }

                when (EditingOptionsValidator.editorOptions[EditingOptionsValidator.editorOptions.size - 1]) {
                    Constants.slowMo -> {
                        val fInput = FFmpegKitConfig.getSafParameterForRead(
                            requireContext(),
                            input
                        )

                        EditingOptionsValidator.editorOptions.clear()
                        EditingOptionsValidator.editorOptions.add(Constants.textOnVideo)
                        // EditingOptionsValidator.commandMap[Constants.slowMo] = ("-y -i $fInput -filter_complex [0:v]trim=0:${startTime / 1000},setpts=PTS-STARTPTS[v1];[0:v]trim=${startTime / 1000}:${endTime / 1000},setpts=$speedUnit*(PTS-STARTPTS)[v2];[0:v]trim=${endTime / 1000},setpts=PTS-STARTPTS[v3];[0:a]atrim=0:${startTime / 1000},asetpts=PTS-STARTPTS[a1];[0:a]atrim=${startTime / 1000}:${endTime / 1000},asetpts=PTS-STARTPTS,atempo=${audioPitch}[a2];[0:a]atrim=${endTime / 1000},asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 -b:v 8192k -vcodec mpeg4 -preset superfast ${dataShareViewModel.outputPath}")
                        val textBitmap = textViewToBitmap(binding.overlayText) // Pass your TextView here
                        val overlayImagePath = "${context?.cacheDir}/text_overlay.png"
                        saveBitmapToFile(textBitmap, overlayImagePath)

                        val videoWidth = binding.ExoPlayer.width
                        val videoHeight = binding.ExoPlayer.height
                        val frameLayoutWidth = binding.frameLayout.width.toFloat()
                        val frameLayoutHeight = binding.frameLayout.height.toFloat()

                        // Calculate scaling factors for width and height
                        val widthRatio = videoWidth / frameLayoutWidth
                        val heightRatio = videoHeight / frameLayoutHeight

                        val overlayX = (binding.overlayText.x - binding.frameLayout.left) * widthRatio
                        val overlayY = (binding.overlayText.y - binding.frameLayout.top) * heightRatio

                        val (x, y) = binding.ExoPlayer.getOverlayTextPosition()
                        println("Overlay Text Position - X: $x, Y: $y")

//                        val scaledOverlayX = overlayX * videoWidth / frameLayoutWidth
//                        val scaledOverlayY = overlayY * videoHeight / frameLayoutHeight
                        try {
//                            val (overlayX, overlayY) = getOverlayPositionForFFmpeg(
//                                binding.overlayText,
//                                binding.frameLayout,
//                                binding.ExoPlayer,
//                                fInput
//                            )

                            val command = "-y -i $fInput -i $overlayImagePath -filter_complex [0:v][1:v]overlay=${binding.ExoPlayer.overlayTextView.x}:${binding.ExoPlayer.overlayTextView.y * 2} -codec:a copy ${dataShareViewModel.outputPath}"
                            Log.d(TAG, "commandSetterMyCommand: $command")
                            EditingOptionsValidator.commandMap[Constants.textOnVideo] = command

                            dataShareViewModel.isChangeApplied = true
                            executor()
                        }catch (e:Exception){
                            Log.d(TAG, "commandSetterMyCommand: ERROR ${e.localizedMessage}")
                        }
                    }

                    Constants.trim -> {
                        Log.d(TAG, "commandSetterDataTrim: $input")
                        dataShareViewModel.params = VideoEditParams(
                            dataShareViewModel.name,
                            input!!,
                            dataShareViewModel.outputPath,
                            trimStartTime,
                            trimEndTime,
                            mainViewModel.duration
                        )
                        dataShareViewModel.isChangeApplied = true
                        executor()
                    }
                }
                dataShareViewModel.isChangeApplied = true
                Log.d(TAG, "commandSetter: Command Added ${dataShareViewModel.outputPath}")
            } catch (e: Exception) {
                Log.d(TAG, "commandSetter: ${e.localizedMessage}")
            }
        }
    }
    private fun getOverlayPositionForFFmpeg(
        overlayText: View,
        frameLayout: FrameLayout,
        playerView: PlayerView,
        videoPath: String
    ): Pair<Float, Float> {
        // Step 1: Get intrinsic video dimensions using MediaMetadataRetriever
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(videoPath)
        val intrinsicVideoWidth = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val intrinsicVideoHeight = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        mediaMetadataRetriever.release()

        // Step 2: Get dimensions of the FrameLayout where video is displayed
        val frameLayoutWidth = frameLayout.width.toFloat()
        val frameLayoutHeight = frameLayout.height.toFloat()

        // Step 3: Calculate aspect ratios
        val videoAspectRatio = intrinsicVideoWidth / intrinsicVideoHeight.toFloat()
        val viewAspectRatio = frameLayoutWidth / frameLayoutHeight

        // Step 4: Calculate video display size within PlayerView, accounting for letterboxing
        val (videoDisplayWidth, videoDisplayHeight) = if (viewAspectRatio > videoAspectRatio) {
            // Horizontal letterboxing (video fits by height)
            val height = frameLayoutHeight
            val width = height * videoAspectRatio
            width to height
        } else {
            // Vertical letterboxing (video fits by width)
            val width = frameLayoutWidth
            val height = width / videoAspectRatio
            width to height
        }

        // Step 5: Calculate offset to account for letterboxing
        val videoOffsetX = (frameLayoutWidth - videoDisplayWidth) / 2
        val videoOffsetY = (frameLayoutHeight - videoDisplayHeight) / 2

        // Step 6: Convert overlayText coordinates to video coordinates
        val overlayTextX = overlayText.x - videoOffsetX
        val overlayTextY = overlayText.y - videoOffsetY

        val overlayX = (overlayTextX / videoDisplayWidth) * intrinsicVideoWidth
        val overlayY = (overlayTextY / videoDisplayHeight) * intrinsicVideoHeight

        return overlayX to overlayY
    }

    private fun textViewToBitmap(textView: TextView): Bitmap {
        // Measure and layout the TextView
        textView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        textView.layout(0, 0, textView.measuredWidth, textView.measuredHeight)

        // Create a bitmap with transparent background
        val bitmap = Bitmap.createBitmap(
            textView.width,
            textView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED)
        textView.draw(canvas)

        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap, filePath: String) {
        val file = File(filePath)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun outputPathGenerator() {
        try {
            Log.d(TAG, "outputPathGenerator: begin")
            val currentTimestamp = System.currentTimeMillis()
            name = "SlowMoMaker$currentTimestamp.mp4"

            var filePath: String = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val storageManager: StorageManager =
                    requireActivity().getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager
                val storageVolume = storageManager.storageVolumes[0]
                val dir = File(storageVolume.directory!!.path + "/DCIM/SlowMoMaker")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                filePath = File(dir, name).absolutePath

            } else {
                val outputDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val slowMoVideoDirectory = File(outputDirectory, "SlowMoMaker")
                if (!slowMoVideoDirectory.exists()) {
                    slowMoVideoDirectory.mkdirs()
                }
                filePath = "${slowMoVideoDirectory.absolutePath}/$name"
                output = filePath
            }
            Log.d(TAG, "outputPathGenerator: new file path created")

            // Move file from cache to generated path
            try {
                if(dataShareViewModel.outputPath != ""){
                    val cacheFile = File(dataShareViewModel.outputPath)
                    val outputFile = File(filePath)
                    cacheFile.copyTo(outputFile, overwrite = true)
                }
            } catch (e:Exception){}

            output = filePath
            dataShareViewModel.outputPath = output
            Log.d(TAG, "before clear cache path")
            mainViewModel.clearCacheDirectory(requireActivity().cacheDir)
            Log.d(TAG, "after clear cache path")
            lifecycleScope.launch(Dispatchers.IO) {
                delay(2000)
                if (filePath != "") {
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "before media store")
                        mainViewModel.updateMediaStore(
                            requireContext(),
                            filePath,
                            name,
                            completeListener,
                            progressListener
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "outputPathGenerator: ${e.localizedMessage}")
        }
    }

    private val completeListener = MediaScannerConnection.OnScanCompletedListener { path, _ ->
            Log.d(TAG, "Scan completed for $path")
            if (isAdded) {
                try {
                    requireActivity().runOnUiThread{
                        MyApplication.mInstance.adsManager.loadInterstitialAd(requireActivity()){
                            try {
//                                safeNavigate(
//                                    R.id.action_videoEditorFragment_to_resultFragment, R.id.videoEditorFragment
//                                )
//                                findNavController().navigate(R.id.to_resultFragment)
                                val intent = Intent(requireContext() , ResultActivity::class.java)
                                intent.putExtra("outputPath",dataShareViewModel.outputPath)
                                intent.putExtra("tabNumber",dataShareViewModel.tabNumber)
                                startActivity(intent)
                            } catch (e:Exception){
                                Log.d(TAG, "ERROR : ${e.localizedMessage} ")
                            }
                        }
                    }
                } catch (e:Exception) {
                    Log.d(TAG, "ERROR : ${e.localizedMessage} ")
                }
            }
        }

    private val progressListener =
        MediaScannerConnection.OnScanCompletedListener { path, uri -> // Handle progress of scanning here
            Log.d(TAG, "Scanning progress for $path")
        }
}
