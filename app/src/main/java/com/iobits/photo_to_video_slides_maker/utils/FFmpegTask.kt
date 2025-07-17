package com.iobits.photo_to_video_slides_maker.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.iobits.photo_to_video_slides_maker.services.MusicProgressService
import com.iobits.photo_to_video_slides_maker.services.VideoEditorProgressService
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.cancel
import java.util.concurrent.TimeUnit

class FFmpegTask {

    private val TAG = "FFmpegTaskTag"
    var ffmpegSession: FFmpegSession? = null
    companion object {
        var taskProgress = 0
    }

   suspend fun executeCommand(
        command: String,
        job: CompletableJob,
        context: Context,
        finalDuration: Int
    ) : FFmpegSession =
        FFmpegKit.executeAsync(command, { session ->
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(session.returnCode)) {
                // Handle success
                handleSuccess(context,job)
            } else {
                // Handle failure
                handleFailure(context,job)
            }
        }, {
            Log.d(TAG, "executeCommand: $it")
        }) { statistics ->
            calculateProgress(statistics, finalDuration)
        }


    private fun calculateProgress(session: Statistics, duration: Int) {
        val timeUs = session.time
        val time = timeFormat(timeUs.toLong())
        onProgress(time, duration / 1000)
    }

    private fun onProgress(s: String, totalDur: Int) {
        val matchSplit = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (totalDur != 0 && matchSplit.size == 3) {
            val progress: Float = (matchSplit[0].toInt() * 3600 + matchSplit[1].toInt() * 60 + matchSplit[2].toFloat()) / totalDur
            val showProgress = (progress * 100).toInt()
            if (showProgress != taskProgress) {
                taskProgress = showProgress
                Log.d(TAG, "=======PROGRESS======== $showProgress")
            }
        }
    }

    private fun timeFormat(millis: Long): String {
        return String.format(
            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }

    private fun handleSuccess(context: Context,job: CompletableJob) {
        try {
            Log.d(TAG, "Command execution Success")
//            job.complete()
//            job.cancel()
            if(isServiceRunning(context, VideoEditorProgressService::class.java)){
                VideoEditorProgressService.onFinishExecution?.invoke()
            }else{
                MusicProgressService.onFinishExecution?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating media store: ${e.localizedMessage}")
        }
    }
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        val services = manager?.getRunningServices(Int.MAX_VALUE)

        if (services != null) {
            for (service in services) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }


    private fun handleFailure(context: Context,job: CompletableJob) {
        Log.d(TAG, "Command execution failed")
        try {
            job.complete()
            job.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying toast: ${e.localizedMessage}")
        }
    }
}
