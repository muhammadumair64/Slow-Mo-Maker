package com.iobits.photo_to_video_slides_maker.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.iobits.photo_to_video_slides_maker.ui.dataModels.MusicEditParams
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
import kotlin.coroutines.cancellation.CancellationException

class MusicProgressService() : Service() {
    companion object {
        var progressFlow = MutableStateFlow<Int?>(null)
        var onFinishExecution: (() -> Unit)? = null

        @SuppressLint("StaticFieldLeak")
        var mParams: MusicEditParams? = null
        var activityContext: Context? = null

        var onComplete: (() -> Unit)? = null
        fun getStartIntent(context: Context, params: MusicEditParams): Intent {
            mParams = params
            activityContext = context
            return Intent(context, MusicProgressService::class.java)
        }
    }

    var progress = 0
    var serviceJob = Job()
    val TAG = "progressForgroundServiceTAG"
    var handler: Handler? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mParams != null) {
                ffmpegTaskRunner()
        }
        onFinishExecution = {
            serviceJob.complete()
            serviceJob.cancel()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            onComplete?.invoke()
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
    override fun onCreate() {
        super.onCreate()
        updateProgress()
    }
    private fun ffmpegTaskRunner() {
        Log.d(TAG, "ffmpegTaskRunner: Process begin")
        try {
            executeCommand(
                mParams!!.cmd,
                activityContext!!,
                mParams!!.duration
            )
        } catch (e: Exception) {
            e.localizedMessage
        }
    }
    private fun executeCommand(cmd: String, context: Context, duration: Int) {
        Log.d(TAG, "executeCommandDATA: $cmd ")

        val scope = CoroutineScope(Dispatchers.IO + serviceJob)
        val task = FFmpegTask()
        scope.launch {
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
    private fun updateProgress() {
        CoroutineScope(Dispatchers.IO + serviceJob).launch {
            while (true) {
                withContext(Dispatchers.Main) {
                        progress = FFmpegTask.taskProgress
                        progressFlow.emit(progress)
                    Log.d(TAG, "updateProgressFlow: $progress")
//                    val notification = notificationUtil.sendNotification(
//                        progress,
//                        messageTitle = "SlideShow Maker",
//                        messageBody = "Processing Video...",
//                        notificationSound = null,
//                        notificationId = 101,
//                        context = this@MusicProgressService
//                    )
//                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
//                        ServiceCompat.startForeground(this@MusicProgressService,101,notification,
//                            ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
//                    }
                }
                delay(1000)
            }
        }
    }
}
