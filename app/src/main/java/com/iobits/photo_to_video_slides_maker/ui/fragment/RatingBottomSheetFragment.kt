package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.LayoutRateUsBottomSheetBinding
import com.iobits.photo_to_video_slides_maker.managers.AnalyticsManager.logEvent
import com.iobits.photo_to_video_slides_maker.managers.PreferenceManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class RatingBottomSheetFragment : BottomSheetDialogFragment() {
    val binding by lazy {
        LayoutRateUsBottomSheetBinding.inflate(layoutInflater)
    }

    var name : ((String)->Unit)? = null
    var comment1State = false
    var comment2State = false
    var comment3State = false
    var comment4State = false
    var comment5State = false
    var starCount =  5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View  {
         initListeners()

        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        return if (enter) {
            AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom)
        } else {
            AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)
        }
    }
    private fun animateStarsToRating(targetRating: Int) {
        lifecycleScope.launch {
            for (i in 1..targetRating) {
                binding.simpleRatingBar.rating = i.toFloat()
                delay(200)
            }
        }
    }
    private fun initListeners() {

        binding.apply {
            lifecycleScope.launch {
                simpleRatingBar.rating =  1F
                delay(500)
                simpleRatingBar.rating =  2F
                delay(300)
                simpleRatingBar.rating =  3F
                delay(300)
                simpleRatingBar.rating =  4F
                delay(300)
                simpleRatingBar.rating =  5F
            }
            simpleRatingBar.setOnRatingChangeListener { ratingBar, rating, fromUser ->
                if (fromUser) {
                    starCount = rating.toInt()
                    when(rating){
                        1F ->{
                            binding.emoji.setImageResource(R.drawable.emoji1)
                        }
                        2F ->{
                            binding.emoji.setImageResource(R.drawable.emoji2)
                        }
                        3F ->{
                            binding.emoji.setImageResource(R.drawable.emoji3)
                        }
                        4F ->{
                            binding.emoji.setImageResource(R.drawable.emoji4)
                        }
                        5F ->{
                            binding.emoji.setImageResource(R.drawable.emoji5)
                       }
                    }
                    animateStarsToRating(rating.toInt())
                }
            }

            binding.cross.setOnClickListener {
                dismiss()
            }

            comment1.setOnClickListener {
                if(comment1State){
                    comment1State = false
                    it.setBackgroundResource(R.drawable.stroke_btn)
                }else{
                    comment1State = true
                    it.setBackgroundResource(R.drawable.btn_bg)
                }
            }
            comment2.setOnClickListener {
                if(comment2State){
                    comment2State = false
                    it.setBackgroundResource(R.drawable.stroke_btn)
                }else{
                    comment2State = true
                    it.setBackgroundResource(R.drawable.btn_bg)
                }
            }
            comment3.setOnClickListener {
                if(comment3State){
                    comment3State = false
                    it.setBackgroundResource(R.drawable.stroke_btn)
                }else{
                    comment3State = true
                    it.setBackgroundResource(R.drawable.btn_bg)
                }
            }
            comment4.setOnClickListener {
                if(comment4State){
                    comment4State = false
                    it.setBackgroundResource(R.drawable.stroke_btn)
                }else{
                    comment4State = true
                    it.setBackgroundResource(R.drawable.btn_bg)
                }
            }
            comment5.setOnClickListener {
                if(comment5State){
                    comment5State = false
                    it.setBackgroundResource(R.drawable.stroke_btn)
                }else{
                    comment5State = true
                    it.setBackgroundResource(R.drawable.btn_bg)
                }
            }

            Start.setOnClickListener {
                if(starCount > 3){
                    dismiss()
                    logEvent("DRAWER_RATTING_ITEM", null)
                    val url = "https://play.google.com/store/apps/details?id=com.moviemaker.imagestovideo.slowmotion"
                    val i = Intent(Intent.ACTION_VIEW)
                    i.setData(Uri.parse(url))
                    startActivity(i)
                    MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.IS_SHOW_RATE_US,false)
                }else{
                    dismiss()
                }
            }
        }
    }
}
