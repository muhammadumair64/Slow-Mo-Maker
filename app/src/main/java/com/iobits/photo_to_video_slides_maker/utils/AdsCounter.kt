package com.iobits.photo_to_video_slides_maker.utils

object AdsCounter {
    private var showAD = 0
    var showPro = 3
    var rattingCounter = 1
    fun showAd(): Boolean {
        showAD++
        return showAD % 2 == 0
    }
    fun showPro():Boolean{
        showPro++
        return showPro % 4 == 0
    }

    fun isShowRatting(): Boolean {
        rattingCounter++
        return rattingCounter % 5 == 0
    }
}