package com.iobits.photo_to_video_slides_maker.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.ads.AdSize
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.ActivityResultBinding
import com.iobits.photo_to_video_slides_maker.managers.AnalyticsManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.disableMultipleClicking
import com.iobits.photo_to_video_slides_maker.utils.handleBackPress
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.vungle.ads.internal.util.ThreadUtil
import java.io.File

class ResultActivity : AppCompatActivity() {
    val binding by lazy {
        ActivityResultBinding.inflate(layoutInflater)
    }
    private var videoDuration = 0
    val TAG = "ResultFragmentTag"
    var outputPath = ""
    var tabNumber = 0
    private lateinit var player: ExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        AnalyticsManager.logEvent("SUCCESSFULLY_EDIT_VIDEO",null)
        tabNumber = intent.getIntExtra("tabNumber",0)
        outputPath = intent.getStringExtra("outputPath").toString()
        initViews()
        loadAds()
    }
    private fun loadAds() {
        MyApplication.mInstance.adsManager.showBanner(this, AdSize.LARGE_BANNER,binding.adView,this.getString(R.string.ADMOB_BANNER_V2),binding.shimmerLayout);
    }

    private fun initViews(){
        MyApplication.isShowRateUs = true
        val mediaItem = outputPath.let { MediaItem.fromUri(it) }

        videoPlayer(mediaItem)
        binding.apply {
            if(tabNumber == 5) { title.text = "My Videos" }

            playPauseController.setOnClickListener {
                disableMultipleClicking(it)
                if(player.isPlaying){
                    player.pause()
                    playPauseController.setImageResource(R.drawable.play)
                } else {
                    player.release()
                    videoPlayer(mediaItem)
                    playPauseController.setImageResource(R.drawable.pause)
                }
            }

            backBtn.setOnClickListener {
                moveBack()
            }

            home.setOnClickListener {
                player.release()
                disableMultipleClicking(it)
                moveBack()
            }

            share.setOnClickListener {
                shareVideo()
            }

            createNew.setOnClickListener {
                player.release()
                disableMultipleClicking(it)
                moveBack()
            }
        }
    }

    private fun moveBack() {
        try {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
                }catch (e:Exception){
                    Log.d(TAG, "moveBack: ERROR ${e.localizedMessage}")
            }
    }
    private fun videoPlayer(mediaItem: MediaItem?) {
        try{
            player = ExoPlayer.Builder(this).build()
            binding.ExoPlayer.player = player
            if (mediaItem != null) {
                player.setMediaItem(mediaItem)
            }
            // Create a PlaybackParameters object to set the speed to 2x for the specified duration
            val playbackParameters = PlaybackParameters(1f, 1f)
            // Set the PlaybackParameters to the player
            player.playbackParameters = playbackParameters
            player.prepare()
            player.play()
            setController()
        } catch (e:Exception){
            e.localizedMessage
        }
    }

    private fun setController()
    {
        if(outputPath != ""){
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(outputPath)
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
                    ThreadUtil.runOnUiThread {
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

    private fun shareVideo() {
        try {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "video/*" // set MIME type of video
            val outPut = outputPath
            val fileOutPut = File(outPut)
            val uri: Uri = Uri.fromFile(fileOutPut)
            val file = uri.path?.let { File(it) }
            val fileUri = file?.let { FileProvider.getUriForFile(
                this, "com.iobits.photo_to_video_slides_maker.provider",
                it
            )
            }
            if(fileUri != null){
                sharingIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    fileUri
                )
            }
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // grant read permission to the receiver app
            startActivity(Intent.createChooser(sharingIntent, "Share via"))
        }catch (e:Exception){
            Log.d(TAG, "shareVideo: ${e.localizedMessage}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            player.release()
        }catch (e:Exception){
            Log.d(TAG, "onDestroy: ")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if(player.isPlaying){
                player.pause()
            }
        }catch (e:Exception){
            Log.d(TAG, "onDestroy: ")
        }

    }

    override fun onResume() {
        super.onResume()
        try {
            if(!player.isPlaying){
                player.play()
            }
        }catch (e:Exception){
            Log.d(TAG, "onDestroy: ")
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        player.release()
        moveBack()
    }
}