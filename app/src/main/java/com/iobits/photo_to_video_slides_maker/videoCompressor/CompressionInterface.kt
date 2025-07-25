package com.iobits.photo_to_video_slides_maker.videoCompressor

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

/**
 * Created by umair
 */
interface CompressionListener {
    @MainThread
    fun onStart(index: Int)

    @MainThread
    fun onSuccess(index: Int, size: Long, path: String?)

    @MainThread
    fun onFailure(index: Int, failureMessage: String)

    @WorkerThread
    fun onProgress(index: Int, percent: Float)

    @WorkerThread
    fun onCancelled(index: Int)
}

interface CompressionProgressListener {
    fun onProgressChanged(index: Int, percent: Float)
    fun onProgressCancelled(index: Int)
}
