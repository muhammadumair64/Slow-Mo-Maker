package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentReverseBinding
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel

class ReverseFragment : Fragment() {
    val binding by lazy {
        FragmentReverseBinding.inflate(layoutInflater)
    }
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initViews()
        return binding.root
    }

    fun initViews() {
        dataShareViewModel.hideExport?.invoke(true)
        binding.apply {
            dataShareViewModel.apply {
                speedEndingPoint =0
                speedEndingPoint =0
                trimEndingPoint=0
                trimEndingPoint=0
            }
            reverseAudio.setOnClickListener {
                dataShareViewModel.isWithOutAudio = false
                reverseAudio.setBackgroundResource(R.drawable.selected_background)
                reverseWithoutAudio.setBackgroundResource(R.drawable.unselected_background)
            }
            reverseWithoutAudio.setOnClickListener {
                dataShareViewModel.isWithOutAudio = true
                reverseWithoutAudio.setBackgroundResource(R.drawable.selected_background)
                reverseAudio.setBackgroundResource(R.drawable.unselected_background)
            }

            reverse.setOnClickListener {
                dataShareViewModel.clickOnReverse?.invoke()
            }
        }
    }
}