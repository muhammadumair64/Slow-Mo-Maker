package com.iobits.photo_to_video_slides_maker.ui.dataModels

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.hw.photomovie.PhotoMovie
import com.hw.photomovie.PhotoMovieFactory
import com.hw.photomovie.PhotoMoviePlayer
import com.hw.photomovie.render.GLSurfaceMovieRenderer
import com.hw.photomovie.render.GLTextureView

data class ProgressServiceParams(
    val context: Context,
    val requireActivity: FragmentActivity,
    val mPhotoMovie: PhotoMovie<*>,
    val mPhotoMoviePlayer: PhotoMoviePlayer,
    val mMovieRenderer: GLSurfaceMovieRenderer,
    val mMovieType: PhotoMovieFactory.PhotoMovieType,
    val glTexture: GLTextureView,
    val mMusicUri: Uri?,
    val output: String
)