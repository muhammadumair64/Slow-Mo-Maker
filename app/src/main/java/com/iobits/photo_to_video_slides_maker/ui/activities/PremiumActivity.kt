package com.iobits.photo_to_video_slides_maker.ui.activities

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.URLUtil
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.ActivityPremiumBinding
import com.iobits.photo_to_video_slides_maker.managers.BillingManagerV5
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.utils.changeStatusBarColor
import com.iobits.photo_to_video_slides_maker.utils.invisible
import com.iobits.photo_to_video_slides_maker.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PremiumActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPremiumBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changeStatusBarColor(this,R.color.status_bar)
        setContentView(binding.root)

        initViews()
        setAnimation()
    }
    private fun setAnimation() {
        val shake: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.shake)
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) {
                    binding.startBtn.startAnimation(shake) // starts animation
                }
                delay(2000)
            }
        }
    }
    private fun initViews(){
        val paint: TextPaint = binding.goPro.paint
        val width: Float = paint.measureText(binding.goPro.text.toString())
        val textShader: Shader = LinearGradient(0f, 0f, width, binding.goPro.textSize, intArrayOf(
            Color.parseColor("#8B31F3"),
            Color.parseColor("#0DBEFF")), null, Shader.TileMode.CLAMP)

        binding.apply {
            startBtn.setOnClickListener {
                if(proTick.isVisible){
                MyApplication.mInstance.billingManagerV5.subscription(this@PremiumActivity)
                }else{
                    finish()
                }
            }
            goPro.paint.shader = textShader
            close.setOnClickListener {
                finish()
            }
            proCard.setOnClickListener {
                proTick.visible()
                binding.freeTick.invisible()
                freeCard.setBackgroundResource(R.drawable.gray_stroke)
                val layoutParams = freeCard.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.setMargins(10.dpToPx(), 10.dpToPx(), 10.dpToPx(), 10.dpToPx())
                proCard.setBackgroundResource(R.drawable.purple_stroke_with_shadow)
                val layoutParams2 = proCard.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams2.setMargins(5.dpToPx(), 5.dpToPx(), 5.dpToPx(), 5.dpToPx())
            }
            freeCard.setOnClickListener {
                proTick.invisible()
                binding.freeTick.visible()
                proCard.setBackgroundResource(R.drawable.gray_stroke)
                val layoutParams = proCard.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.setMargins(10.dpToPx(), 10.dpToPx(), 10.dpToPx(), 10.dpToPx())
                freeCard.setBackgroundResource(R.drawable.purple_stroke_with_shadow)
                val layoutParams2 = freeCard.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams2.setMargins(5.dpToPx(), 5.dpToPx(), 5.dpToPx(), 5.dpToPx())
            }
            termText.setOnClickListener {
              showTerm(this@PremiumActivity)
            }
            licenseAgreement.setOnClickListener {
                showTerm(this@PremiumActivity)
            }
            privacyPolicy.setOnClickListener {
                showPrivacyPolicy(this@PremiumActivity)
            }

            restoreAccess.setOnClickListener{
                showCancelSub()
            }

            setPrice()
        }
    }

    private fun showCancelSub() {
        try {
            if (URLUtil.isValidUrl(getString(R.string.cancel_subscription_url))) {
                val i = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.cancel_subscription_url))
                )
                this.startActivity(i)
            }
        } catch (e: Exception) {
            Log.d("ERROR", "showCancelSub: ${e.localizedMessage}")
        }
    }

    private fun showPrivacyPolicy(context: Context) {
        if (URLUtil.isValidUrl("https://igniteapps.blogspot.com/2024/02/images-to-video-movie-maker-privacy.html")) {
            val i = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://igniteapps.blogspot.com/2024/02/images-to-video-movie-maker-privacy.html")
            )
            context.startActivity(i)
        }
    }


    private fun showTerm(context: Context) {
        if (URLUtil.isValidUrl("https://igniteapps.blogspot.com/2024/09/terms-conditions-slow-motion-video-maker.html")) {
            val i = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://igniteapps.blogspot.com/2024/09/terms-conditions-slow-motion-video-maker.html")
            )
            context.startActivity(i)
        }
    }


    private fun setPrice() {
        try {
            if(BillingManagerV5.subPremiumPrice.contains("Free")){
                binding.apply {
                    binding.currentPrice.visible()
                    binding.priceMore.text = "Then ${BillingManagerV5.subPremiumPriceAfterDiscount}/week"
                }
            } else {
                binding.priceMore.invisible()
                if(BillingManagerV5.subPremiumPrice.isEmpty()){
                    binding.currentPrice.text = "$2.99 /week"

                }else{
                binding.currentPrice.text = "${BillingManagerV5.subPremiumPrice}/week"
                }
            }
        } catch (e: Exception) {
            Log.d("PRO_ACTIVITY", "ERROR : ${e.localizedMessage}")
        }
    }
    private fun Int.dpToPx(): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }
    override fun onPause() {
        super.onPause()
        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }
}