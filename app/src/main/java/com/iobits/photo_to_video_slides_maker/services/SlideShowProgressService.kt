package com.iobits.photo_to_video_slides_maker.services


import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.app.ServiceCompat

import androidx.fragment.app.FragmentActivity
import com.hw.photomovie.PhotoMovie
import com.hw.photomovie.PhotoMovieFactory
import com.hw.photomovie.PhotoMoviePlayer
import com.hw.photomovie.record.GLMovieRecorder
import com.hw.photomovie.render.GLSurfaceMovieRenderer
import com.hw.photomovie.render.GLTextureView
import com.hw.photomovie.util.MLog
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ProgressServiceParams
import com.iobits.photo_to_video_slides_maker.utils.NotificationUtil
import com.iobits.photo_to_video_slides_maker.utils.UriUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlideShowProgressService() : Service() {
    companion object {
        var progressFlow = MutableStateFlow<Int?>(null)

        @SuppressLint("StaticFieldLeak")
        var mParams: ProgressServiceParams? = null
        var onComplete: ((name: String, path: String) -> Unit)? = null
        fun getStartIntent(context: Context, params: ProgressServiceParams): Intent {
            mParams = params
            return Intent(context, SlideShowProgressService::class.java)
        }
    }

    var filePath: String = ""
    val job = Job()
    var progress = 0
    var serviceJob = Job()
    var fileName = ""
    val TAG = "progressForgroundServiceTAG"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mParams != null) {

            mParams!!.apply {
                saveVideo(
                    MyApplication.appContext,
                    requireActivity,
                    mPhotoMovie,
                    mPhotoMoviePlayer,
                    mMovieRenderer,
                    mMovieType,
                    glTexture, mMusicUri, output
                )
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mParams = null
            job.cancel()
            serviceJob.cancel()
            progressFlow.tryEmit(100)
        } catch (e: Exception) {
            e.localizedMessage
        }

        Log.d(TAG, "Service is Stop now ")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun saveVideo(
        context: Context,
        requireActivity: FragmentActivity,
        mPhotoMovie: PhotoMovie<*>?,
        mPhotoMoviePlayer: PhotoMoviePlayer?,
        mMovieRenderer: GLSurfaceMovieRenderer?,
        mMovieType: PhotoMovieFactory.PhotoMovieType,
        glTexture: GLTextureView,
        mMusicUri: Uri?, output: String
    ) {
        mPhotoMoviePlayer!!.pause()
        val startRecodTime = System.currentTimeMillis()
        val recorder = GLMovieRecorder(requireActivity)

        filePath = output
        val bitrate = if (glTexture.width * glTexture.height > 1000 * 1500) 8000000 else 4000000
        recorder.configOutput(
            glTexture.width,
            glTexture.height,
            bitrate,
            30,
            1,
            output
        )
        val newPhotoMovie =
            PhotoMovieFactory.generatePhotoMovie(mPhotoMovie!!.photoSource, mMovieType)
        val newMovieRenderer = GLSurfaceMovieRenderer(mMovieRenderer)
        newMovieRenderer.photoMovie = newPhotoMovie
        var audioPath: String? = null
        if (mMusicUri != null) {
            audioPath = UriUtil.getPath(context, mMusicUri)
        }
        if (!TextUtils.isEmpty(audioPath)) {
            recorder.setMusic(audioPath)
        }
        recorder.setDataSource(newMovieRenderer)

        recorder.startRecord(object : GLMovieRecorder.OnRecordListener {
            override fun onRecordFinish(success: Boolean) {
                val recordEndTime = System.currentTimeMillis()
                MLog.i("Record", "record:" + (recordEndTime - startRecodTime))
                if (success) {
                    onComplete?.invoke(fileName, filePath)
                    stopSelf()

                } else {
                    Toast.makeText(
                        requireActivity.applicationContext,
                        "error!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                if (recorder.audioRecordException != null) {
                    Toast.makeText(
                        requireActivity.applicationContext,
                        "record audio failed:" + recorder.audioRecordException.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onRecordProgress(recordedDuration: Int, totalDuration: Int) {
                //  dialog.progress = (recordedDuration / totalDuration.toFloat() * 100).toInt()
                progress = (recordedDuration / totalDuration.toFloat() * 100).toInt()
            }
        })
    }

    override fun onCreate() {
        super.onCreate()
        updateProgress()
    }


    private fun updateProgress() {
        CoroutineScope(serviceJob).launch(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) {
                    progressFlow.emit(progress)
                    Log.d(TAG, "updateProgressFlow: ${progress}")

//                    val notification = notificationUtil.sendNotification(
//                        progress,
//                        messageTitle = "SlideShow Maker",
//                        messageBody = "Processing Video...",
//                        notificationSound = null,
//                        notificationId = 101,
//                        context = this@SlideShowProgressService
//                    )
//                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
//                        ServiceCompat.startForeground(this@SlideShowProgressService,101,notification,
//                            ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
//                    }
                    delay(1000)
                }
            }
        }
    }
}
