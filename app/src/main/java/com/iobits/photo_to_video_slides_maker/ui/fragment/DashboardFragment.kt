package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.hw.photomovie.record.gles.GlUtil.TAG
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentDashboardSecondBinding
import com.iobits.photo_to_video_slides_maker.managers.AnalyticsManager
import com.iobits.photo_to_video_slides_maker.managers.GoogleMobileAdsConsentManager
import com.iobits.photo_to_video_slides_maker.managers.PreferenceManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.activities.PremiumActivity
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.utils.AdsCounter
import com.iobits.photo_to_video_slides_maker.utils.Constants
import com.iobits.photo_to_video_slides_maker.utils.EditingOptionsValidator
import com.iobits.photo_to_video_slides_maker.managers.RewardAdManager
import com.iobits.photo_to_video_slides_maker.utils.disableMultipleClicking
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.handleLastBackPress
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.utils.showEmailChooser
import com.iobits.photo_to_video_slides_maker.utils.showSettingsDialog
import com.iobits.photo_to_video_slides_maker.utils.watchAdOrBuyPremium
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean


class DashboardFragment : Fragment() {

//    val binding by lazy {
//        FragmentDashboardBinding.inflate(layoutInflater)
//    }

    val binding by lazy {
        FragmentDashboardSecondBinding.inflate(layoutInflater)
    }

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private var itemClick = 0
    private val mainViewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private val notificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            if (isGranted.containsValue(false)) {
                showSettingsDialog()
            } else {
               mainViewModel.loadAudios(requireContext())
                moveNext()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        itemClick = 0
        initViews()
        showConsent()
        return binding.root
    }

    private fun loadAds() {
        try{
        MyApplication.mInstance.adsManager.showBanner(requireContext(), AdSize.LARGE_BANNER,binding.adView,requireContext().getString(R.string.ADMOB_BANNER_V2),binding.shimmerLayout)
        }catch (e:Exception){
            e.localizedMessage
        }
    }

    override fun onResume() {
        super.onResume()
        handleLastBackPress {
            requireActivity().moveTaskToBack(true)
          }

        dataShareViewModel.clearData()
        mainViewModel.clearCacheDirectory(requireActivity().cacheDir)

        if (AdsCounter.showPro()) {
            if (!MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)){
                startActivity(Intent(requireContext(), PremiumActivity::class.java))
            }
        }
        if(MyApplication.mInstance.isComingFromAction){
            if(MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_SHOW_RATE_US,true)){
             openRatingBottomSheet()
            }
        }

    }
    private fun openRatingBottomSheet() {
        val bottomSheetFragment = RatingBottomSheetFragment()
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
    }
    private fun initViews() {
        if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)){
            binding.apply {
                crown1.gone()
                crown2.gone()
                crown3.gone()
                proAnim.gone()
            }
        }

        val shake: android.view.animation.Animation? = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) {
                    binding.createNew.startAnimation(shake) // starts animation
                }
                delay(4000)
            }
        }
        /** clear data */
        dataShareViewModel.clearData()

        binding.apply {
            createSlideShow.setOnClickListener {
                if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)){
                    AnalyticsManager.logEvent("CLICK_ON_SLIDE_SHOW",null)
                    disableMultipleClicking(it)
                    itemClick = 1
                    dataShareViewModel.tabNumber = 2
                    launchPermission()
                } else {
                    watchAdOrBuyPremium(requireActivity(),
                        onCloseClick = {}, onWatchAd = {
                            lifecycleScope.launch {
                                RewardAdManager.showRewardedAd(requireContext(),requireActivity()){
                                    AnalyticsManager.logEvent("CLICK_ON_SLIDE_SHOW",null)
                                    disableMultipleClicking(it)
                                    itemClick = 1
                                    dataShareViewModel.tabNumber = 2
                                    launchPermission()
                                }
                            }
                        }, onBuyPremium = {
                            AdsCounter.showPro = 1
                            startActivity(Intent(requireContext(), PremiumActivity::class.java))
                        })

                }
            }
            proAnim.setOnClickListener {
                AdsCounter.showPro = 1
                startActivity(Intent(requireContext(), PremiumActivity::class.java))
            }

            speed.setOnClickListener {
                if(AdsCounter.showAd()){
                    MyApplication.mInstance.adsManager.loadInterstitialAd(requireActivity()){
                        AnalyticsManager.logEvent("CLICK_ON_SLOW_MO",null)
                        dataShareViewModel.tabNumber = 3
                        dataShareViewModel.soloEditor = Constants.slowMo
                        disableMultipleClicking(it)
                        itemClick = 3
                        launchPermission()
                    }
                }else{
                    AnalyticsManager.logEvent("CLICK_ON_SLOW_MO",null)
                    dataShareViewModel.tabNumber = 3
                    dataShareViewModel.soloEditor = Constants.slowMo
                    disableMultipleClicking(it)
                    itemClick = 3
                    launchPermission()
                }

            }

            videoEditor.setOnClickListener {
                if(AdsCounter.showAd()){
                    MyApplication.mInstance.adsManager.loadInterstitialAd(requireActivity()){
                        AnalyticsManager.logEvent("CLICK_ON_FULL_VIDEO_EDITOR",null)
                        dataShareViewModel.tabNumber = 1
                        dataShareViewModel.soloEditor = ""
                        disableMultipleClicking(it)
                        itemClick = 5
                        launchPermission()
                    }
                }else{
                    AnalyticsManager.logEvent("CLICK_ON_FULL_VIDEO_EDITOR",null)
                    dataShareViewModel.tabNumber = 1
                    dataShareViewModel.soloEditor = ""
                    disableMultipleClicking(it)
                    itemClick = 5
                    launchPermission()
                }

            }

            trim.setOnClickListener {
                if(AdsCounter.showAd()){
                    MyApplication.mInstance.adsManager.loadInterstitialAd(requireActivity()){
                        AnalyticsManager.logEvent("CLICK_ON_TRIM",null)
                        dataShareViewModel.tabNumber = 4
                        dataShareViewModel.soloEditor = Constants.trim
                        disableMultipleClicking(it)
                        itemClick = 2
                        launchPermission()
                    }
                }else{
                    AnalyticsManager.logEvent("CLICK_ON_TRIM",null)
                    dataShareViewModel.tabNumber = 4
                    dataShareViewModel.soloEditor = Constants.trim
                    disableMultipleClicking(it)
                    itemClick = 2
                    launchPermission()
                }

            }

            myVideos.setOnClickListener {
                if(AdsCounter.showAd()){
                    MyApplication.mInstance.adsManager.loadInterstitialAd(requireActivity()){
                        AnalyticsManager.logEvent("CLICK_ON_MY_CREATION",null)
                        dataShareViewModel.tabNumber = 5
                        disableMultipleClicking(it)
                        itemClick = 4
                        launchPermission()
                    }
                }else{
                    AnalyticsManager.logEvent("CLICK_ON_MY_CREATION",null)
                    dataShareViewModel.tabNumber = 5
                    disableMultipleClicking(it)
                    itemClick = 4
                    launchPermission()
                }

            }

            compressor.setOnClickListener {
                if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)){
                    AnalyticsManager.logEvent("CLICK_ON_COMPRESS_VIDEO",null)
                    dataShareViewModel.tabNumber = 6
                    dataShareViewModel.soloEditor = ""
                    disableMultipleClicking(it)
                    itemClick = 6
                    launchPermission()
                }else{
                    watchAdOrBuyPremium(requireActivity(),
                        onCloseClick = {}, onWatchAd = {
                            lifecycleScope.launch {
                                RewardAdManager.showRewardedAd(requireContext(),requireActivity()){
                                    AnalyticsManager.logEvent("CLICK_ON_COMPRESS_VIDEO",null)
                                    dataShareViewModel.tabNumber = 6
                                    dataShareViewModel.soloEditor = ""
                                    disableMultipleClicking(it)
                                    itemClick = 6
                                    launchPermission()
                                }
                            }
                        }, onBuyPremium = {
                            AdsCounter.showPro = 1
                            startActivity(Intent(requireContext(), PremiumActivity::class.java))
                        })

                }
            }
            reverseVideo.setOnClickListener {
                if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)){
                    EditingOptionsValidator.editorOptions.add(Constants.reverse)
                    AnalyticsManager.logEvent("CLICK_ON_REVERSE_VIDEO",null)
                    dataShareViewModel.tabNumber = 7
                    dataShareViewModel.soloEditor = Constants.reverse
                    EditingOptionsValidator.isUsingOnlyReverse = true
                    disableMultipleClicking(it)
                    itemClick = 7
                    launchPermission()
                }else{
                    AdsCounter.showPro = 1
                    startActivity(Intent(requireContext(), PremiumActivity::class.java))
                }
            }

            menu.setOnClickListener {
                if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START))
                    binding.drawerLayout.openDrawer(GravityCompat.START)
            }

            binding.drawerLayout.setScrimColor((getColor(requireContext(), R.color.textColor)))
        }

        drawerMenu()
    }

    private fun drawerMenu() {
        binding.navigationLayout.apply {
            goPremium.setOnClickListener {
                binding.drawerLayout.closeDrawers()
                startActivity(Intent(requireContext(), PremiumActivity::class.java))
            }
            customerSupport.setOnClickListener {
                binding.drawerLayout.closeDrawers()
                if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)){
                    customerSupport()
                }else{
                    MyApplication.mInstance.isOutForRateUs = true
                    AdsCounter.showPro = 1
                    startActivity(Intent(requireContext(), PremiumActivity::class.java))
                }

            }
            privacyPolicy.setOnClickListener {
                MyApplication.mInstance.isOutForRateUs = true
                binding.drawerLayout.closeDrawers()
                showPrivacyPolicy(requireContext())
            }
            share.setOnClickListener {
                MyApplication.mInstance.isOutForRateUs = true
                binding.drawerLayout.closeDrawers()
                AnalyticsManager.logEvent("CLICK_ON_SHARE",null)
                shareApp()
            }
            rateUs.setOnClickListener {
                MyApplication.mInstance.isOutForRateUs = true
                binding.drawerLayout.closeDrawers()
                rateUs()
            }
        }
    }

    private fun showPrivacyPolicy(context: Context) {
        try {
            if (URLUtil.isValidUrl("https://igniteapps.blogspot.com/2024/02/images-to-video-movie-maker-privacy.html")) {
                val i = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://igniteapps.blogspot.com/2024/02/images-to-video-movie-maker-privacy.html")
                )
                context.startActivity(i)
            }
        }catch (e:Exception){
            Log.d(TAG, "showPrivacyPolicy: ${e.localizedMessage}")}
    }

    private fun moveNext() {
        when (itemClick) {
            1 -> {
                safeNavigate(
                    R.id.action_dashboardFragment_to_galleryFragment,
                    R.id.dashboardFragment
                )
            }

            else -> {
                safeNavigate(
                    R.id.action_dashboardFragment_to_videoGalleryFragment,
                    R.id.dashboardFragment
                )
            }
        }
    }

    private fun showConsent() {
        // Log the Mobile Ads SDK version.
        // Log the Mobile Ads SDK version.
        Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

        googleMobileAdsConsentManager =
            GoogleMobileAdsConsentManager.getInstance(requireContext())
        googleMobileAdsConsentManager.gatherConsent(
            requireActivity()
        ) { consentError ->
            if (consentError != null) {
                // Consent not obtained in current session.
                Log.w(
                    TAG,
                    String.format(
                        "%s: %s",
                        consentError.getErrorCode(),
                        consentError.getMessage()
                    )
                )
            }
            if (googleMobileAdsConsentManager.canRequestAds()) {
                initializeMobileAdsSdk()
            }
            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired()) {
                // Regenerate the options menu to include a privacy setting.
                requireActivity().invalidateOptionsMenu()
            }
        }

        // This sample attempts to load ads using consent obtained in the previous session.

        // This sample attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        Log.d(TAG, "initializeMobileAdsSdk: call ")
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        Log.d(TAG, "initializeMobileAdsSdk: App instance")
        // Initialize the Mobile Ads SDK.
        MyApplication.mInstance.adsManager.initSDK(requireContext()) {
            Log.d(TAG, "initializeMobileAdsSdk: successful")
            // call here
            loadAds()
        }

    }

    private fun launchPermission() {
        notificationsPermissionLauncher.launch(getRequiredPermissions())
    }

    private fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES
                ).plus(getPostNotificationsPermission())
            }

            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun getPostNotificationsPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            // Return a dummy permission for older Android versions
            "dummy_permission"
        }
    }

    private fun customerSupport() {
        val supportEmail = "arbax8031@gmail.com" // Replace with your support email address
        val subject = "Support"
        val feedback = "Calculator Vault"
        requireContext().showEmailChooser(supportEmail, subject, feedback)
    }

    private fun rateUs() {
        try {
            val url = "https://play.google.com/store/apps/details?id=com.moviemaker.imagestovideo.slowmotion"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            startActivity(intent)
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    private fun shareApp() {
        Log.d(tag, "IN function")
        Toast.makeText(requireContext(), getString(R.string.please_wait), Toast.LENGTH_SHORT).show()
        try {
            val url = "https://play.google.com/store/apps/details?id=com.moviemaker.imagestovideo.slowmotion"
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.app_name)))
        } catch (ex: Exception) {
            Toast.makeText(requireContext(), "Sorry process can't Completed", Toast.LENGTH_SHORT)
                .show()
        }
    }
}
