package com.iobits.photo_to_video_slides_maker.services


import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.dataModels.VideoEditParams
import com.iobits.photo_to_video_slides_maker.utils.Constants
import com.iobits.photo_to_video_slides_maker.utils.EditingOptionsValidator
import com.iobits.photo_to_video_slides_maker.utils.FFmpegTask
import com.iobits.photo_to_video_slides_maker.utils.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

class VideoEditorProgressService() : Service() {

    companion object {
        var progressFlow = MutableStateFlow<Int?>(null)
        var onFinishExecution: (() -> Unit)? = null

        @SuppressLint("StaticFieldLeak")
        var mParams: VideoEditParams? = null
        var activityContext: Context? = null

        var onComplete: ((name: String, path: String) -> Unit)? = null
        fun getStartIntent(context: Context, params: VideoEditParams): Intent {
            mParams = params
            activityContext = context
            return Intent(context, VideoEditorProgressService::class.java)
        }
    }
    var progressFactor = 1
    var progress = 0
    var trimmerProgress = 0
    var serviceJob = Job()
    val TAG = "progressForgroundServiceTAG"
    var handler: Handler? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mParams != null) {
            FFmpegTask.taskProgress =    0
            if (EditingOptionsValidator.isUsingOnlyTrimmer) {
                mParams!!.apply {
                    Log.d(
                        TAG,
                        "onStartCommand: Trimmer Data $output ...$name ====== $startPoint ==== $endPoint "
                    )
                    trimmer(name, input, output, startPoint, endPoint)
                }
            } else {
                progressFactor = EditingOptionsValidator.editorOptions.size
                ffmpegTaskRunner()
            }
        }
        onFinishExecution = {
            CoroutineScope(Dispatchers.IO).launch {
                delay(2000)
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "executeCommand: CallBack Invoke success")
                    if (EditingOptionsValidator.editorOptions.isNotEmpty()) {
                        ffmpegTaskRunner()
                    } else {
                        Log.d(TAG, "executeCommand: before stopSelf")
                        stopSelf()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            onComplete?.invoke(mParams!!.name, mParams!!.output)
            mParams = null
            serviceJob.cancel()
            progressFlow.tryEmit(100)
            handler?.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.localizedMessage
        }
        Log.d(TAG, "Service is Stop now ")
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        updateProgress()
    }
    //---------------------------------------- FFMPEG -------------------------------------------//
    private fun ffmpegTaskRunner() {
        Log.d(TAG, "ffmpegTaskRunner: Process begin")
        val task = EditingOptionsValidator.editorOptions[0]
        if (task == Constants.trim) {
            mParams!!.apply {
                trimmer(name, input, output, startPoint, endPoint)
                Log.d(TAG, "ffmpegTaskRunner: Trimmer Data ${mParams!!.output}")
            }
        } else {
            executeCommand(
                EditingOptionsValidator.commandMap[task]!!,
                activityContext!!,
                mParams!!.duration
            )
            try {

            } catch (e: Exception) {

                Log.d(TAG, "ffmpegTaskRunner: ERROR ${e.localizedMessage}")
            }
        }
        EditingOptionsValidator.editorOptions.remove(task)
    }
    private fun executeCommand(cmd: String, context: Context, duration: Int) {
        Log.d(TAG, "executeCommandDATA: $cmd ")

        val scope = CoroutineScope(Dispatchers.IO + serviceJob)
        val task = FFmpegTask()

        scope.launch {
            Log.d(TAG, "executeCommand: MY COMMAND IS $cmd")
            val session = task.executeCommand(cmd, serviceJob, context, duration)
            task.ffmpegSession = session
            FFmpegTask.taskProgress = 0
            session.failStackTrace
        }
        serviceJob.invokeOnCompletion {
            if (it is CancellationException) {
                FFmpegKitConfig.clearSessions()
                Log.d(TAG, "executeCommand: after session Clear")
                stopSelf()
                Log.d(TAG, "executeCommand: ${EditingOptionsValidator.editorOptions.size}")
                serviceJob.cancel()
            }
        }
    }
    //---------------------------------------- Trimmer -------------------------------------------//
    @OptIn(UnstableApi::class)
    @SuppressLint("SuspiciousIndentation")
    fun trimmer(name: String, videoUri: Uri, output: String, startPoint: Int, endPoint: Int) {
        val completed = AtomicBoolean(false)
        trimmerProgress = 0
        val transformerListener: Transformer.Listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                Log.d(TAG, "onCompleted: complete")
                completed.set(true)
            }
            override fun onError(
                composition: Composition, result: ExportResult, exception: ExportException
            ) {
                completed.set(true)
            }
        }
        val file = videoUri.path?.let { File(it) }
        /** Media Item */
        Log.d(TAG, "trimmerDATA: $file ===== ${Uri.fromFile(file)} ")
        val inputMediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startPoint.toLong())
                    .setEndPositionMs(endPoint.toLong())
                    .build()
            ).build()
        val editedItem = EditedMediaItem.Builder(inputMediaItem).build()
        val transformer = Transformer.Builder(this)
            .setVideoMimeType(MimeTypes.VIDEO_H265)
            .addListener(transformerListener)
            .build()
        transformer.start(editedItem, output)
        val progressHolder = ProgressHolder()
        handler = Handler(Looper.getMainLooper())

        val trimmerRunnable = object : Runnable {
            override fun run() {
                if (!completed.get()) {
                    trimmerProgress = when (transformer.getProgress(progressHolder)) {
                        Transformer.PROGRESS_STATE_NOT_STARTED -> 0
                        else -> progressHolder.progress.toFloat().toInt()
                    }
                    Log.d(TAG, "trimmerProgress: $trimmerProgress")

                    handler!!.postDelayed(this, 1000)
                } else {
                    handler!!.removeCallbacksAndMessages(null)
                    if(EditingOptionsValidator.isUsingOnlyTrimmer){
//                  onComplete?.invoke(name, output)
                    stopSelf()
                    }else{
                    onFinishExecution?.invoke()
                    }
                }
            }
        }
        handler!!.post(trimmerRunnable)
    }
    //---------------------------------------- Progress ------------------------------------------//

    private fun updateProgress() {
        CoroutineScope(Dispatchers.IO + serviceJob).launch {
            while (true) {
                withContext(Dispatchers.Main) {
                    if (EditingOptionsValidator.isUsingOnlyTrimmer) {
                        progressFlow.emit(trimmerProgress)
                    } else {
                        val ffmpegProgress = FFmpegTask.taskProgress / progressFactor
                        if(ffmpegProgress  != 0) {
                            progressFlow.emit( progress + ffmpegProgress)
                        } else {
                             progress = trimmerProgress / progressFactor
                            progressFlow.emit(progress + ffmpegProgress)
                        }
                    }
                    Log.d(TAG, "updateProgressFlow: ${progress}")
//                    val notification = notificationUtil.sendNotification(
//                        progress,
//                        messageTitle = "SlideShow Maker",
//                        messageBody = "Processing Video...",
//                        notificationSound = null,
//                        notificationId = 101,
//                        context = this@VideoEditorProgressService
//                    )
//                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
//                        ServiceCompat.startForeground(this@VideoEditorProgressService,101,notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
//                    }
                }
                delay(1000)
            }
        }
    }
}
