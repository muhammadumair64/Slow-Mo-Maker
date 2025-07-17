package com.iobits.photo_to_video_slides_maker.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.util.Log
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.iobits.photo_to_video_slides_maker.databinding.ActivitySplashBinding
import com.iobits.photo_to_video_slides_maker.managers.PreferenceManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.visible

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }
    val TAG = "SplashActivityTag"

    private var interAdLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContentView(binding.root)
        initViews()
        if (!MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.IS_APP_ADS_FREE,
                false
            )){
            showInterAd()
            }else{
            Handler(Looper.getMainLooper()).postDelayed({
                openMainActivity()
            }, 4000)

        }
    }

    private fun showInterAd(){
        MyApplication.mInstance.adsManager.loadInterstitialAdAutoSplash(this, adLoaded = {
            interAdLoaded = true
        }, failedToLoadAd = {
            interAdLoaded = false
        })
        val SPLASH_TIME_OUT = 6000
        Handler(Looper.getMainLooper()).postDelayed({
            if (interAdLoaded) MyApplication.mInstance.adsManager.showInterstitialAd(this){
                startActivity(Intent(this@SplashActivity,MainActivity::class.java))
                finish()
            }
            else {
                binding.getStart.visible()
                binding.loadingAnim.gone()
                binding.getStart.setOnClickListener {
                    MyApplication.mInstance.adsManager.loadInterstitialAd(this){
                        startActivity(Intent(this@SplashActivity,MainActivity::class.java))
                        finish()
                    }
                }
            }

        }, SPLASH_TIME_OUT.toLong())
    }

    private fun initOpenAd() {
       // MyApplication.mInstance.loadOpenAdManually(this)
//        MyApplication.mInstance.adsManager.loadInterstitialAdWithoutLoading(this)
        Handler(Looper.getMainLooper()).postDelayed({
            if (MyApplication.mInstance.preferenceManager.getBoolean(
                    PreferenceManager.Key.IS_APP_ADS_FREE,
                    false
                )
            ) {
                openMainActivity()
            } else {
                // Show the app open ad.
                MyApplication.mInstance.showAdIfAvailable(
                    this@SplashActivity,
                    object : MyApplication.OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            openMainActivity()
                        }
                    })
            }
        }, 6000)
    }


    private fun showNextBtn(){
        binding.getStart.visible()
        binding.nextBtn.setOnClickListener {
            MyApplication.mInstance.adsManager.showInterstitialAdWithoutLoading(this){
                openMainActivity()
            }
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun initViews(){
        val paint: TextPaint = binding.appName.paint
        val width: Float = paint.measureText(binding.appName.text.toString())
        val textShader: Shader = LinearGradient(0f, 0f, width, binding.appName.textSize, intArrayOf(
            Color.parseColor("#8B31F3"),
            Color.parseColor("#0DBEFF")), null, Shader.TileMode.CLAMP)

        binding.appName.paint.shader = textShader
    }

    override fun onResume() {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}