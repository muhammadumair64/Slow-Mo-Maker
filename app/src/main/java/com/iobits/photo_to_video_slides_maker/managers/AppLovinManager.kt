package com.iobits.photo_to_video_slides_maker.managers

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkConfiguration
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.managers.interfaces.CheckAddShowing
import com.iobits.photo_to_video_slides_maker.managers.interfaces.IBannerCallBack
import com.iobits.photo_to_video_slides_maker.managers.interfaces.INativeCallBack
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLovinManager @Inject constructor(context: Context) {
    enum class NativeAdType {
        MEDIUM_TYPE, SMALL_TYPE
    }

    private var interstitialAd: MaxInterstitialAd? = null
    private var retryAttempt = 0
    private val adView: MaxAdView? = null
    private val context: Context = context

    //native
    private lateinit var nativeAdLoader: MaxNativeAdLoader
    private var nativeAd: MaxAd? = null

    //native
    private lateinit var nativeSmallAdLoader: MaxNativeAdLoader
    private var nativeSmallAd: MaxAd? = null

    private var showInterstitialCheck = true

    private fun checkSdkKey() {
        val sdkKey = AppLovinSdk.getInstance(MyApplication.mInstance).sdkKey
        if ("YOUR_SDK_KEY" == sdkKey) {
            AlertDialog.Builder(MyApplication.mInstance).setTitle("ERROR")
                .setMessage("Please update your sdk key in the manifest file.").setCancelable(false)
                .setNeutralButton("OK", null).show()
        }
    }

    fun initializeApplovinSDK() {
        // Initialize the SDK.
        AppLovinSdk.getInstance(context).mediationProvider = AppLovinMediationProvider.MAX
        AppLovinSdk.getInstance(context).settings.testDeviceAdvertisingIds = listOf(
            "76367bdc-1f69-4e48-8884-c8cec3c0849f",
            "5eaef36d-6f20-442d-a195-5efb5c0b4646",
            "38cc0dfb-f018-43c4-8aaf-48a9d7b42be5",
            "3f40b8a9-931e-44e4-a175-d62bd679266e",
            "89fff254-bb0e-4133-a4da-61bdcc121818",
           // "b046a82c-6cf3-4852-b8f2-72c6dbd26f59"
        )
        AppLovinSdk.initializeSdk(context) { _: AppLovinSdkConfiguration? -> }
        // Check that SDK key is present in Android Manifest
        // checkSdkKey();

    }

    val isSDKInitialized: Boolean
        get() = AppLovinSdk.getInstance(context).isInitialized

    // Interstitial AD
    fun loadInterstitialAd(activity: Activity, func: () -> Unit) {


        if (isAppAdFree()!!) {
            func.invoke()
            return
        }
        if (!AdEventManager.isInternetAvailable()) {
            func.invoke()
            return
        }


        try {

//                AdEventManager.initialize(activity)

            AdEventManager.handleAdEvent(activity, AdState.InitAdCall, func)

            interstitialAd = MaxInterstitialAd(
                MyApplication.mInstance?.resources?.getString(R.string.APP_LOVIN_INTERSTITIAL), activity
            )
            interstitialAd?.setListener(object : MaxAdListener {
                override fun onAdLoaded(ad: MaxAd) {
                    Log.d(TAG, "onAdLoaded: ")
                    // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'
                    AdEventManager.handleAdEvent(activity, AdState.Loaded, func)

                    // Reset retry attempt
                    retryAttempt = 0
                }

                override fun onAdDisplayed(ad: MaxAd) {
                    showInterstitialCheck = false
                    resetInterstitialWaitTime();
                    AdEventManager.handleAdEvent(activity, AdState.Displayed, func)
                    retryAttempt = 0
                    Log.d(TAG, "onAdDisplayed: ")
                }

                override fun onAdHidden(ad: MaxAd) {
                    // Interstitial ad is hidden. Pre-load the next ad
                    Log.d(TAG, "onAdHidden: ")
                    AdEventManager.handleAdEvent(activity, AdState.Hidden, func)
                //                    interstitialAd?.loadAd()
                }

                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    // Interstitial ad failed to load
                    // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)
                    retryAttempt++
                    if (retryAttempt > 2) {
                        AdEventManager.handleAdEvent(activity, AdState.LoadFailed, func)
                        retryAttempt = 0
                    } else {
                        interstitialAd?.loadAd()
                    }

                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    // Interstitial ad failed to display. We recommend loading the next ad

                    AdEventManager.handleAdEvent(activity, AdState.DisplayFailed, func)
//
//                    interstitialAd?.loadAd()
                }
            })

            // Load the first ad
            if (interstitialAd?.isReady == true) {
                AdEventManager.handleAdEvent(activity, AdState.Loaded, func)
            }else{
                interstitialAd?.loadAd()
            }
        } catch (e: Exception) {
            Log.d(TAG, "loadInterstitialAd: ")
        }

    }

    fun showInterstitialAd(
        checkAddShowing: CheckAddShowing? = null
    ) {
        if (isAppAdFree()!!) {
            return
        }
//         check if 10 sec time has been passed

        if (!showInterstitialCheck) return;

        try {
            if (interstitialAd?.isReady == true) {
                interstitialAd?.showAd()
            }
        } catch (e: Exception) {
            Log.d(TAG, "showInterstitialAd: ")
        }
    }

    private fun resetInterstitialWaitTime() {
//        Handler(Looper.getMainLooper()).postDelayed({ showInterstitialCheck = true }, 15000)
        Handler(Looper.getMainLooper()).postDelayed({ showInterstitialCheck = true }, 1000)
    }

    fun loadNativeAd(
        nativeAdContainer: FrameLayout, AdType: NativeAdType, iNativeCallBack: INativeCallBack
    ) {
        if (isAppAdFree()!!) {
            iNativeCallBack.onFailed()
            return
        }

        nativeAdLoader = if (AdType == NativeAdType.SMALL_TYPE) {
            MaxNativeAdLoader(
                context.resources.getString(R.string.APP_LOVIN_SMALL_NATIVE), context
            )
        } else {
            MaxNativeAdLoader(
                context.resources.getString(R.string.APP_LOVIN_MEDIUM_NATIVE), context
            )
        }

        nativeAdLoader.setNativeAdListener(object : MaxNativeAdListener() {

            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                // Clean up any pre-existing native ad to prevent memory leaks.
                if (nativeAd != null) {
                    nativeAdLoader.destroy(nativeAd)
                }

                // Save ad for cleanup.
                nativeAd = ad

                // Add ad view to view.
                nativeAdContainer.removeAllViews()
                nativeAdContainer.addView(nativeAdView)

                iNativeCallBack.onLoad()
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                // We recommend retrying with exponentially higher delays up to a maximum delay
                iNativeCallBack.onFailed()
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                // Optional click callback
            }
        })
        nativeAdLoader.loadAd()
    }


    fun loadSmallNativeAd(
        activity: Activity, frameLayout: FrameLayout, iNativeCallBack: INativeCallBack?
    ) {
        if (isAppAdFree()!!) {
            iNativeCallBack?.onFailed()
            Log.d(TAG, "showBannerAd: returned")
            return
        }
        val nativeAdContainer = frameLayout
        nativeSmallAdLoader = MaxNativeAdLoader(
            MyApplication.mInstance?.resources?.getString(R.string.APP_LOVIN_SMALL_NATIVE), context
        )

        nativeSmallAdLoader.setNativeAdListener(object : MaxNativeAdListener() {

            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                // Clean up any pre-existing native ad to prevent memory leaks.
                if (nativeSmallAd != null) {
                    nativeSmallAdLoader.destroy(nativeSmallAd)
                }

                // Save ad for cleanup.
                nativeSmallAd = ad

                // Add ad view to view.
                nativeAdContainer.removeAllViews()
                nativeAdContainer.addView(nativeAdView)
                //callback
                iNativeCallBack?.onLoad()
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                Log.d("onNativeAdLoadFailed", "onNativeAdLoadFailed: " + error.message)
                // We recommend retrying with exponentially higher delays up to a maximum delay
                iNativeCallBack?.onFailed()
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                // Optional click callback
            }
        })
        nativeSmallAdLoader.loadAd()
    }

    fun showBannerAd(adView: MaxAdView, iBannerCallBack: IBannerCallBack?) {
        Log.d(TAG, "showBannerAd: ")
        if (isAppAdFree()!!) {
            iBannerCallBack?.onFailed()
            Log.d(TAG, "showBannerAd: returned")
            return
        }

        try {
            adView.setListener(object : MaxAdViewAdListener {
                override fun onAdExpanded(ad: MaxAd) {
                    Log.d(TAG, "onAdExpanded: ")
                }

                override fun onAdCollapsed(ad: MaxAd) {
                    Log.d(TAG, "onAdCollapsed: ")
                }

                override fun onAdLoaded(ad: MaxAd) {
                    iBannerCallBack?.onLoad()
                    Log.d(TAG, "onAdLoaded: ")
                }

                override fun onAdDisplayed(ad: MaxAd) {
                    Log.d(TAG, "onAdDisplayed: ")
                }

                override fun onAdHidden(ad: MaxAd) {
                    Log.d(TAG, "onAdHidden: ")
                }

                override fun onAdClicked(ad: MaxAd) {
                    Log.d(TAG, "onAdClicked: ")
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    iBannerCallBack?.onFailed()
                    Log.d(TAG, "onAdLoadFailed: ")
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    iBannerCallBack?.onFailed()
                    Log.d(TAG, "onAdDisplayFailed: ")
                }
            })

            // Load the ad
            adView.loadAd()
        } catch (e: Exception) {
            Log.d(TAG, "showBannerAd: ")
        }

    }

    companion object {
        private const val TAG = "AppLovinManager"
        const val ARE_ADS_ENABLED = "ARE_ADS_ENABLED"
    }

    private fun isAppAdFree() = MyApplication.mInstance?.preferenceManager?.getBoolean(
        PreferenceManager.Key.IS_APP_ADS_FREE, false
    )
}