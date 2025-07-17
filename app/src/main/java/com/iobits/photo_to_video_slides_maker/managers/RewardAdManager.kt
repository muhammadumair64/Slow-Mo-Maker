package com.iobits.photo_to_video_slides_maker.managers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication


object RewardAdManager {

    private var showRewardedInterstitialAd = true
    private var rewardAdCountDownTimer: CountDownTimer? = null

    fun showRewardedAd(context: Context, activity: Activity, onAdSuccess: () -> Unit) {
        val rewardLoadingDialog = createLoadingDialog(context)
        rewardAdCountDownTimer = object : CountDownTimer(7000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                rewardLoadingDialog?.findViewById<TextView>(R.id.progress_msg)?.text= "Loading ad in ${millisUntilFinished / 1000} seconds ..."
            }

            override fun onFinish() {
                showRewardedInterstitialAd = false
                if (rewardLoadingDialog != null) {
                    dismissLoadingDialog(rewardLoadingDialog)
                }
                showToast(context, activity.getString(R.string.reward_ad_not))
            }
        }

        rewardLoadingDialog?.show()
        if (rewardLoadingDialog?.window != null) {
            rewardLoadingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        rewardAdCountDownTimer?.start()
        rewardLoadingDialog?.let { loadRewardedAd(context, activity, it, onAdSuccess) }
    }

    private fun createLoadingDialog(context: Context): AlertDialog? {
        val builder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_ad_loading, null)
        builder.setView(dialogView)
        return builder.setCancelable(false).create()
    }

    private fun loadRewardedAd(
        context: Context,
        activity: Activity,
        rewardLoadingDialog: AlertDialog,
        onAdSuccess: () -> Unit
    ) {
        showRewardedInterstitialAd = true
        MyApplication.mInstance.adsManager.loadRewardVideoAd(activity, object : AdsManager.IRewardVideo {
            override fun onFailedToLoad() {
                dismissLoadingDialog(rewardLoadingDialog)
                cancelTimer()
                showToast(context, activity.getString(R.string.reward_ad_not))
            }

            override fun onRewardVideoLoad() {
                if (showRewardedInterstitialAd) {
                    MyApplication.mInstance.adsManager.showRewardVideoAd(activity, this)
                    cancelTimer() // Cancel the timer as the ad is loading
                }
                dismissLoadingDialog(rewardLoadingDialog)
            }

            override fun onFailedToShow() {
                dismissLoadingDialog(rewardLoadingDialog)
                cancelTimer()
                showToast(context, activity.getString(R.string.reward_ad_not))
            }

            override fun onRewardedSuccess() {
                dismissLoadingDialog(rewardLoadingDialog)
                cancelTimer()
                onAdSuccess()
            }
        })
    }

    private fun cancelTimer() {
        rewardAdCountDownTimer?.cancel()
        rewardAdCountDownTimer = null
    }

    private fun dismissLoadingDialog(dialog: AlertDialog) {
        if (dialog.isShowing) dialog.dismiss()
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
