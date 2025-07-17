package com.iobits.photo_to_video_slides_maker.ui.dataModels

import android.graphics.Bitmap
import android.net.Uri

data class AudioDataClass(
    var title: String,
    val id: String,
    val album: String = "",
    val duration: Long = 0,
    val size: String,
    var path: String,
    var artUri: Uri,
    var albumArtPath: Bitmap?  // Added albumArtPath field
)
