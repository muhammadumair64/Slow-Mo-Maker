package com.iobits.photo_to_video_slides_maker.videoCompressor.video

data class Result(
    val index: Int,
    val success: Boolean,
    val failureMessage: String?,
    val size: Long = 0,
    val path: String? = null,
)
