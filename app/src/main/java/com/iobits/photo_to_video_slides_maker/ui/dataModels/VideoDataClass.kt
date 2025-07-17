package com.iobits.photo_to_video_slides_maker.ui.dataModels

import android.net.Uri


data class VideoDataClass(val id: String, var title: String, val duration: Long = 0, val folderName: String, val size: String
                          , var path: String, var artUri: Uri
)