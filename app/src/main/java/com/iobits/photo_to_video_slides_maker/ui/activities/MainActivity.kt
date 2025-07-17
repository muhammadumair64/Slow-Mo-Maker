package com.iobits.photo_to_video_slides_maker.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.ActivityMainBinding
import com.iobits.photo_to_video_slides_maker.databinding.ActivityPremiumBinding
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.utils.changeStatusBarColor
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        changeStatusBarColor(this, R.color.background_color)
//        startActivity(Intent(this,PremiumActivity::class.java))
    }
}
