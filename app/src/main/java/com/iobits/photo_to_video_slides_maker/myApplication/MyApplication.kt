package com.iobits.photo_to_video_slides_maker.myApplication

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.managers.AdsManager
import com.iobits.photo_to_video_slides_maker.managers.AppLovinManager
import com.iobits.photo_to_video_slides_maker.managers.BillingManagerV5
import com.iobits.photo_to_video_slides_maker.managers.PreferenceManager
import com.iobits.photo_to_video_slides_maker.ui.activities.PremiumActivity
import com.iobits.photo_to_video_slides_maker.ui.activities.SplashActivity
import dagger.hilt.android.HiltAndroidApp
import java.util.Date
import javax.inject.Inject

@HiltAndroidApp
class MyApplication: Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var adsManager: AdsManager
    @Inject
    lateinit var appLovinManager: AppLovinManager
    @Inject
    lateinit var billingManagerV5: BillingManagerV5

    var showIAP = true

    var isInterShowing = false
    var isOutForRateUs = false
    var isComingFromAction = false

    private lateinit var appOpenAdManager: AppOpenAdManager

    private lateinit var currentActivity: Activity

    private val LOG_TAG = "MyApplication"

    var openAdRecentlyShown = false

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        mContext = applicationContext
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        appContext = applicationContext



        // appLovinManager.initializeApplovinSDK()
        // Get a reference to the window and set the status bar icon and text color to dark
    }

    //open ad
    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
//        currentActivity?.let { appOpenAdManager.showAdIfAvailable(it) }
        if (isInterShowing) return

        if(isOutForRateUs){
            isOutForRateUs = false
            return
        }

        when (currentActivity) {
            is AdActivity -> {}
            is SplashActivity -> {}
            is PremiumActivity -> {}
            else -> {
                appOpenAdManager.showAdIfAvailable(currentActivity)
            }
        }

    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    /**
     * since we are showing open ad in only splash screen
     * and not in onResume
     * so we have to load manually
     * */
    fun loadOpenAdManually(activity: Activity){
        appOpenAdManager.loadAd(activity)
        Log.d(LOG_TAG, "loadOpenAdManually: ")
    }

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
     * dismissed or fails to show).
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    /** Inner class that loads and shows app open ads. */
    private inner class AppOpenAdManager {

        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false


        /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
        private var loadTime: Long = 0

        /**
         * Load an ad.
         *
         * @param context the context of the activity that loads the ad
         */
        fun loadAd(context: Context) {
            // if one time ad is shown don't send the next load request
//            if (oneTimeAdShown)
//                return
            if (preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false))
                return

            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                getString(R.string.ADMOD_OPEN_AD),
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    /**
                     * Called when an app open ad has loaded.
                     *
                     * @param ad the loaded app open ad.
                     */
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(LOG_TAG, "onAdLoaded.")
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.message)
                    }
                }
            )
        }

        /** Check if ad was loaded more than n hours ago. */
        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        /** Check if ad exists and can be shown. */
        private fun isAdAvailable(): Boolean {
            // Ad references in the app open beta will time out after four hours, but this time limit
            // may change in future beta versions. For details, see:
            // https://support.google.com/admob/answer/9341964?hl=en
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         */
        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that shows the ad.
                    }
                }
            )
        }
        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
         */
        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener
        ) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.")
                return
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }

            Log.d(LOG_TAG, "Will show ad.")

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                /** Called when full screen content is dismissed. */
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
//                    oneTimeAdShown = true
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(LOG_TAG, "onAdDismissedFullScreenContent.")


                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                    openAdRecentlyShown = true
                }

                /** Called when fullscreen content failed to show. */
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.message)

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                /** Called when fullscreen content is shown. */
                override fun onAdShowedFullScreenContent() {
                    Log.d(LOG_TAG, "onAdShowedFullScreenContent.")
                }
            }
            isShowingAd = true
            appOpenAd!!.show(activity)
        }
    }

    companion object{
        var isShowRateUs =  false
        private lateinit var mContext: Context

        lateinit var mInstance: MyApplication

        var isShowOpenAD = true

        lateinit var appContext: Context
        var currentProcessActivity = ""
    }
}