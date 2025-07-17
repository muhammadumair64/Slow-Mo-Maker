package com.iobits.photo_to_video_slides_maker.managers

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoController.VideoLifecycleCallbacks
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.utils.Constants
import com.iobits.photo_to_video_slides_maker.utils.Constants.Companion.ITEM_SKU_REMOVE_ADS_ONLY
import com.iobits.photo_to_video_slides_maker.utils.invisible
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext context: Context?,
    private val preferenceManager: PreferenceManager
) {
    enum class NativeAdType {
        REGULAR_TYPE, MEDIUM_TYPE, SMALL_TYPE, NOMEDIA_MEDIUM
    }

    private var admobInterstitialAd: InterstitialAd? = null
    private val TAG: String = AdsManager::class.java.name

    private var mRewardedAd: RewardedAd? = null
    private var alertDialogAds: AlertDialog? = null

    private fun showAdLoadingDialog(context: Activity?) {
        try {
            val dialogBuilder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_ad_loading, null)
            dialogBuilder.setView(dialogView)
            alertDialogAds = dialogBuilder.create()
            alertDialogAds?.setCancelable(false)
            alertDialogAds?.show()
            if (alertDialogAds?.window != null) {
                alertDialogAds?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    fun hideDialogAds() {
        try {
            if (alertDialogAds != null && alertDialogAds!!.isShowing) {
                alertDialogAds!!.dismiss()
            }
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    fun initSDK(context: Context?, isdKinit: () -> Unit) {
        MobileAds.initialize(context!!) { initializationStatus: InitializationStatus ->
            Log.d(TAG, "AdsManager: initializes")
            // for bidding
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(
                    "MyApp", String.format(
                        "Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status!!.description, status.latency
                    )
                )
            }
        }
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                mutableListOf(
                    "F58A121CA6B48BE487E841B65137F635",
                    "F489CD7B6E9F529144AEEBD5D32D3417",
                    "47BD8D71811EF0D2F25B46868A922D56",
                    "32F9DA89E49FBDF09C1F69F89FF071E0"
                )
            )
                .build()
        )

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(context) { initializationStatus: InitializationStatus? ->
            Log.d(TAG, "AdsManager: initializes")
            isdKinit.invoke()
        }
    }


    private fun prepareAdRequest(): AdRequest {
        val adRequest = AdRequest.Builder().build()
        return adRequest
    }


    fun loadInterstitialAd(activity: Activity, funcToInvoke: Runnable) {
        if (!preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            if (isNetWorkAvailable(activity)) {
                showAdLoadingDialog(activity)
                if (admobInterstitialAd != null) {
                    showInterstitialAd(activity, funcToInvoke)
                    return
                }
                // Create a timer for a 10-second timeout
                val handler = Handler()
                val timeoutRunnable = Runnable {
                    hideDialogAds()
                    funcToInvoke.run()
                }
                handler.postDelayed(timeoutRunnable, 10000) // Delay for 10 seconds


                val interstitialAdLoadCallback: InterstitialAdLoadCallback =
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            super.onAdLoaded(interstitialAd)
                            Log.d(TAG, "onLoad: admob interstitial")
                            handler.removeCallbacks(timeoutRunnable) // Remove timer if ad loads
                            admobInterstitialAd = interstitialAd
                            showInterstitialAd(activity, funcToInvoke)
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            super.onAdFailedToLoad(loadAdError)
                            Log.d(TAG, "onAdFailedToLoad: admob interstitial. Loading facebook ad")
                            handler.removeCallbacks(timeoutRunnable) // Remove timer on failure
                            hideDialogAds()
                            funcToInvoke.run()
                        }
                    }
                InterstitialAd.load(
                    activity,
                    activity.getString(R.string.ADMOB_INTERSTITIAL_V2),
                    prepareAdRequest(),
                    interstitialAdLoadCallback
                )
            } else {
                funcToInvoke.run()
            }
        } else {
            funcToInvoke.run()
        }
    }

    fun showInterstitialAd(activity: Activity?, funcAfterAdHidden: Runnable) {
        Log.d(TAG, "showInterstitialAd: Show start")
        if (admobInterstitialAd != null) {
            admobInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                      funcAfterAdHidden.run();
                    hideDialogAds()
                    Log.d(TAG, "onAdDismissedFullScreenContent: ")
                    admobInterstitialAd = null
                    MyApplication.mInstance.isInterShowing = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    hideDialogAds()
                    super.onAdFailedToShowFullScreenContent(adError)
                    Log.d(TAG, "onAdFailedToShowFullScreenContent: ")
                    funcAfterAdHidden.run()
                }

                override fun onAdShowedFullScreenContent() {
                    hideDialogAds()
                    MyApplication.mInstance.isInterShowing = true
                    super.onAdShowedFullScreenContent()
                }
            }
            admobInterstitialAd!!.show(activity!!)
        } else {
            hideDialogAds()
            Log.d(TAG, "showInterstitialAd: No Inter")
            funcAfterAdHidden.run()
        }
    }


    //------------------------------------------ Non loading -------------------------------------//
    fun loadInterstitialAdAutoSplash(activity: Activity, adLoaded: ()-> Unit, failedToLoadAd: ()-> Unit) {
        if (!preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            if (isNetWorkAvailable(activity)) {
                // showOpenAd = false
                val interstitialAdLoadCallback: InterstitialAdLoadCallback =
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            super.onAdLoaded(interstitialAd)
                            Log.d(TAG, "onLoad: admob interstitial")
                            admobInterstitialAd = interstitialAd
                            adLoaded.invoke()
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            super.onAdFailedToLoad(loadAdError)
                            Log.d(TAG, "onAdFailedToLoad: admob interstitial. Loading facebook ad")
                            //  showOpenAd = true
                            failedToLoadAd.invoke()
                        }
                    }
                InterstitialAd.load(
                    activity,
                    activity.getString(R.string.ADMOB_INTERSTITIAL_V2),
                    prepareAdRequest(),
                    interstitialAdLoadCallback
                )
            }else{
                // showOpenAd = true
                failedToLoadAd.invoke()
            }
        }else{
            //   showOpenAd = true
            failedToLoadAd.invoke()
        }
    }

    fun loadInterstitialAdWithoutLoading(activity: Activity) {
        if (!preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            if (isNetWorkAvailable(activity)) {
                val interstitialAdLoadCallback: InterstitialAdLoadCallback =
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            super.onAdLoaded(interstitialAd)
                            Log.d(TAG, "onLoad: admob interstitial")
                            admobInterstitialAd = interstitialAd
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            super.onAdFailedToLoad(loadAdError)
                            Log.d(TAG, "onAdFailedToLoad: admob interstitial. Loading facebook ad")
                        }
                    }
                InterstitialAd.load(
                    activity,
                    activity.getString(R.string.ADMOB_INTERSTITIAL_V2),
                    prepareAdRequest(),
                    interstitialAdLoadCallback
                )
            }
        }
    }

    fun showInterstitialAdWithoutLoading(activity: Activity?, funcAfterAdHidden: Runnable) {
        if (!preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            if (admobInterstitialAd != null) {
                admobInterstitialAd!!.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            Log.d(TAG, "onAdDismissedFullScreenContent: ")
                            admobInterstitialAd = null
                            MyApplication.mInstance.isInterShowing = false
                            //                      resetInterstitialWaitTime();
                            funcAfterAdHidden.run()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            super.onAdFailedToShowFullScreenContent(adError)
                            Log.d(TAG, "onAdFailedToShowFullScreenContent: ")
                            funcAfterAdHidden.run()
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            MyApplication.mInstance.isInterShowing = true
                        }
                    }
                admobInterstitialAd!!.show(activity!!)
            } else {
                funcAfterAdHidden.run()
            }
        }
    }

    private fun getAdSize(activity: Activity): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    fun loadNativeAd(
        activity: Activity,
        frameLayout: FrameLayout?,
        nativeAdViewType: NativeAdType,
        nativeAdId: String?,
        shimmer: ShimmerFrameLayout
    ) {
        if (!isNetWorkAvailable(activity.baseContext)) {
            frameLayout!!.visibility = View.GONE
            shimmer.invisible()
        }
        if (!preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            shimmer.startShimmer()
            val builder = AdLoader.Builder(activity, nativeAdId!!)
            builder.forNativeAd { nativeAd: NativeAd ->
                var nativeAdView: NativeAdView? = null
                if (nativeAdViewType == NativeAdType.REGULAR_TYPE) nativeAdView =
                    activity.layoutInflater.inflate(
                        R.layout.admob_native_regular_layout,
                        null
                    ) as NativeAdView
                if (nativeAdViewType == NativeAdType.MEDIUM_TYPE) nativeAdView =
                    activity.layoutInflater.inflate(
                        R.layout.admob_native_medium_layout,
                        null
                    ) as NativeAdView
                if (nativeAdViewType == NativeAdType.SMALL_TYPE) nativeAdView =
                    activity.layoutInflater.inflate(
                        R.layout.admob_native_small_layout,
                        null
                    ) as NativeAdView
                if (nativeAdViewType == NativeAdType.NOMEDIA_MEDIUM) nativeAdView =
                    activity.layoutInflater.inflate(
                        R.layout.admob_native_nomedi_medium_layout,
                        null
                    ) as NativeAdView
//                if (nativeAdViewType == NativeAdType.NOMEDIA_MEDIUM) nativeAdView =
//                    activity.layoutInflater.inflate(
//                        R.layout.admob_native_nomedi_medium_layout,
//                        null
//                    ) as NativeAdView
                nativeAdView?.let { populateUnifiedNativeAdView(nativeAd, it, nativeAdViewType, activity) }
                if (frameLayout == null) {
                    Log.d(TAG, "FRAME_LAYOUT_NULL: ")
                } else {
                    frameLayout.removeAllViews()
                    frameLayout.addView(nativeAdView)
                    Log.d(TAG, "onNativeAdLoaded: ")
                }
            }
            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
            builder.withNativeAdOptions(adOptions)
            val adLoader = builder.withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.d(TAG, "onAdFailedToLoad: $loadAdError")
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        shimmer.stopShimmer()
                        shimmer.hideShimmer()
                    }
                })
                .build()
            adLoader.loadAd(prepareAdRequest())
        } else {
            frameLayout!!.visibility = View.GONE
        }
    }

    private fun populateUnifiedNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        nativeAdType: NativeAdType,
        activity: Activity
    ) {
        // Set the media view.
        if (nativeAdType == NativeAdType.REGULAR_TYPE || nativeAdType == NativeAdType.MEDIUM_TYPE || nativeAdType == NativeAdType.NOMEDIA_MEDIUM) adView.mediaView =
            adView.findViewById(R.id.ad_media)

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)

        try {
            adView.findViewById<View>(R.id.remove_ad_btn).setOnClickListener { l: View? ->
                Toast.makeText(
                    activity.applicationContext,
                    activity.getString(R.string.please_wait),
                    Toast.LENGTH_SHORT
                ).show()
                MyApplication.mInstance?.billingManagerV5?.oneTimePurchase(
                    activity,
                    Constants.ITEM_SKU_REMOVE_ADS_ONLY
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "populateUnifiedNativeAdView: " + e.localizedMessage)
        }

        // The headline and mediaContent are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView?)!!.text = nativeAd.headline
        if (nativeAdType == NativeAdType.REGULAR_TYPE || nativeAdType == NativeAdType.MEDIUM_TYPE || nativeAdType == NativeAdType.NOMEDIA_MEDIUM) adView.mediaView!!.mediaContent =
            nativeAd.mediaContent

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            Log.d(TAG, "populateUnifiedNativeAdView: " + nativeAd.body)

            if (nativeAdType == NativeAdType.NOMEDIA_MEDIUM) {
                adView.bodyView?.visibility = View.GONE
            }else adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView?)!!.text = nativeAd.body

        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.INVISIBLE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            (adView.callToActionView as Button?)!!.text = nativeAd.callToAction
        }
        if (nativeAd.icon == null) {
            adView.iconView!!.visibility = View.GONE
        } else {
            (adView.iconView as ImageView?)!!.setImageDrawable(
                nativeAd.icon!!.drawable
            )
            adView.iconView!!.visibility = View.VISIBLE
        }
        if (nativeAd.store == null) {
            adView.storeView!!.visibility = View.INVISIBLE
        } else {
            adView.storeView!!.visibility = View.VISIBLE
            (adView.storeView as TextView?)!!.text = nativeAd.store
        }

        /*  if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }*/

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
        // have a video asset.
        val vc = nativeAd.mediaContent!!.videoController

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc.hasVideoContent()) {
            vc.videoLifecycleCallbacks = object : VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    super.onVideoEnd()
                }
            }
        }
    }


    // **********Reward Video *********//
    fun loadRewardVideoAd(activity: Activity, iRewardVideo: IRewardVideo) {
//        if (!preferenceManager.getBoolean(PreferenceManager.Key.REMOTE_CONFIG_IS_AD_ENABLE, true)) {
//            return;
//        }
        RewardedAd.load(
            activity,
            activity.getString(R.string.ADMOB_REWARD_VIDEO),
            prepareAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    Log.d(TAG, "$loadAdError RewardVideoAd")
                    mRewardedAd = null
                    iRewardVideo.onFailedToLoad()
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    Log.d(TAG, "RewardVideoAd Ad was loaded.")
                    iRewardVideo.onRewardVideoLoad()
                }
            })
    }

    fun showRewardVideoAd(activity: Activity?, iRewardVideo: IRewardVideo) {
        mRewardedAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, " RewardVideoAd Ad dismissed fullscreen content.")
                mRewardedAd = null
                iRewardVideo.onRewardedSuccess()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, " RewardVideoAd Ad failed to show fullscreen content.")
                mRewardedAd = null
                iRewardVideo.onFailedToShow()
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "RewardVideoAd Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "RewardVideoAd Ad showed fullscreen content.")
            }
        }

        if (mRewardedAd != null) {
            mRewardedAd!!.show(activity!!) { rewardItem -> // Handle the reward.
                Log.d(TAG, "The user earned the reward.")
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
        }
    }

    interface IRewardVideo {
        fun onFailedToLoad()

        fun onRewardVideoLoad()

        fun onFailedToShow()

        fun onRewardedSuccess()
    }
    
    // ********* BANNER ADS ***********//
    fun showBanner(
        context: Context,
        size: AdSize?,
        adFrame: FrameLayout,
        adId: String?,
        shimmer: ShimmerFrameLayout
    ) {
        if (preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            adFrame.visibility = View.GONE
            return
        }
        Log.d("BANNER_AD", "BANNER_AD_FUN")

        shimmer.startShimmer()
        if (isNetWorkAvailable(context)) {
            val adView = AdView(context)
            adView.setAdSize(size!!)
            adView.adUnitId = adId!!
            adFrame.addView(adView)
            adView.loadAd(prepareAdRequest())

            adView.adListener = object : AdListener() {
                override fun onAdClicked() {
                    super.onAdClicked()
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    shimmer.stopShimmer()
                    shimmer.hideShimmer()
                    shimmer.visibility = View.INVISIBLE
                    Log.d("AD_DEBUG", "onAdLoaded: Banner Load")
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                }
            }
        }else{
            adFrame.visibility = View.GONE
            shimmer.stopShimmer()
            shimmer.hideShimmer()
            shimmer.visibility = View.INVISIBLE
        }
    }


    fun showAdaptiveBanner(
        activity: Activity,
        adViewContainer: FrameLayout,
        shimmer: ShimmerFrameLayout
    ) {
        if (MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.IS_APP_ADS_FREE,
                false
            )
        ) {
            adViewContainer.visibility = View.GONE
            shimmer.visibility = View.GONE
            return
        }
        if (isNetWorkAvailable(activity.applicationContext)) {
            shimmer.startShimmer()
            val adView = AdView(activity)
            adView.adUnitId = activity.getString(R.string.ADMOB_BANNER_V2)
            adViewContainer.removeAllViews()
            adViewContainer.addView(adView)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    shimmer.stopShimmer()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    adViewContainer.visibility = View.GONE
                    shimmer.visibility = View.GONE
                }
            }
            adView.setAdSize(getAdSize(activity))
            val extras = Bundle()
            extras.putString("collapsible", "bottom")
            val adRequest = AdRequest.Builder().addNetworkExtrasBundle(
                AdMobAdapter::class.java, extras
            ).build()
            adView.loadAd(adRequest)
        } else {
            adViewContainer.visibility = View.GONE
            shimmer.visibility = View.GONE
        }
    }

    //    private void saveAdActivityInPref(PreferenceManager.Key key) {
    //        int oldValue = preferenceManager.getInt(key, 0);
    //        int newValue = oldValue + 1;
    //        preferenceManager.put(key, newValue);
    //    }
    /**
     * Rewarded Interstitial Ads
     */
    fun showCollapsableBanner(
        activity: Activity,
        adViewContainer: FrameLayout,
        shimmer: ShimmerFrameLayout
    ) {
        if (MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.IS_APP_ADS_FREE,
                false
            )
        ) {
            adViewContainer.visibility = View.GONE
            return
        }
        if (isNetWorkAvailable(activity.applicationContext)) {
            shimmer.startShimmer()
            val adView = AdView(activity)
            adView.adUnitId = activity.getString(R.string.ADMOB_BANNER_COLLAPSIBLE)
            adViewContainer.removeAllViews()
            adViewContainer.addView(adView)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    shimmer.stopShimmer()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    adViewContainer.visibility = View.GONE
                }
            }

            adView.setAdSize(getAdSize(activity))
            val extras = Bundle()
            extras.putString("collapsible", "bottom")
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()
            adView.loadAd(adRequest)
        } else {
            adViewContainer.visibility = View.GONE
        }
    }

    interface ISDKinit {
        fun onInitialized()
    }

    companion object {
        //-------------------------------------------------------------------------------------------------//
        fun isNetWorkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetWorkInfo = connectivityManager.activeNetworkInfo
            return activeNetWorkInfo != null && activeNetWorkInfo.isConnected
        }
    }
}