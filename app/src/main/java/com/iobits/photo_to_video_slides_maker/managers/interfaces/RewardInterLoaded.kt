package com.iobits.photo_to_video_slides_maker.managers.interfaces

import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

interface RewardInterLoaded {
    fun onRewardedInterstitialLoaded(ad: RewardedInterstitialAd? = null)
}