package com.iobits.photo_to_video_slides_maker.ui.dataModels

import android.net.Uri

data class VideoEditParams(
    val name: String, val input: Uri, val output: String, val startPoint: Int, val endPoint: Int, var duration: Int = 0
)