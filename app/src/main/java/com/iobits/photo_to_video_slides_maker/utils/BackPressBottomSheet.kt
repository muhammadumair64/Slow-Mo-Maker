package com.iobits.photo_to_video_slides_maker.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.managers.AdsManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication

class BackPressBottomSheet : BottomSheetDialogFragment() {

    private var shimmerFrameLayout: ShimmerFrameLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bottom_sheet, container, false)

        val closeButton = view.findViewById<TextView>(R.id.confirm_to_exit)
        val adFrame = view.findViewById<FrameLayout>(R.id.ad_frame)
        shimmerFrameLayout = view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout)

        closeButton.setOnClickListener {
            activity?.finishAffinity()
        }

//   Load native ad and handle shimmer visibility

//        MyApplication.mInstance.adsManager.loadNativeAd(
//            activity,
//            adFrame,
//            com.iobits.photo_to_video_slides_maker.managers.AdsManager.NativeAdType.MEDIUM_TYPE,
//            getString(R.string.ADMOB_NATIVE_WITH_MEDIA_V2),
//            shimmerFrameLayout
//        )

        return view
    }


}
