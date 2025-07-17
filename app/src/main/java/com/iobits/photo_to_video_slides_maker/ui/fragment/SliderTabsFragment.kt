package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentSliderTabsBinding
import com.iobits.photo_to_video_slides_maker.managers.AnalyticsManager
import com.iobits.photo_to_video_slides_maker.managers.PreferenceManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.activities.PremiumActivity
import com.iobits.photo_to_video_slides_maker.ui.adapters.EditorImageAdapter
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.AdsCounter
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate

class SliderTabsFragment : Fragment() {

    val binding by lazy {
        FragmentSliderTabsBinding.inflate(layoutInflater)
    }
    private var imageAdapter: EditorImageAdapter? = null
    private val viewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        imageAdapter = EditorImageAdapter(requireContext())
        initListeners()
        imageRV()
        return binding.root
    }

    private fun initListeners(){
        if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)){
            binding.apply {
                crown3.gone()
            }
        }
        binding.addImages.setOnClickListener {
            dataShareViewModel.onAddImageClick?.invoke()
        }
        binding.apply {
            transitions.setOnClickListener {
                AnalyticsManager.logEvent("CLICK_ON_TRANSITIONS",null)
                if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)){
                    safeNavigate(R.id.action_sliderTabsFragment_to_transitionFragment, R.id.sliderTabsFragment)
                }else{
                    AdsCounter.showPro = 1
                    startActivity(Intent(requireContext(), PremiumActivity::class.java))
                }

            }
            filters.setOnClickListener {
                AnalyticsManager.logEvent("CLICK_ON_FILTERS",null)
                safeNavigate(R.id.action_sliderTabsFragment_to_effectsFragment, R.id.sliderTabsFragment)
            }
            music.setOnClickListener {
                AnalyticsManager.logEvent("CLICK_ON_SLIDER_MUSIC",null)
            dataShareViewModel.onMusicTabClick?.invoke()
            }
        }
    }

    private fun imageRV(){
        binding.editorImageRV.apply {
            layoutManager= LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL ,false)
            adapter = imageAdapter
        }
        imageAdapter?.apply {
            updateList(dataShareViewModel.selectedImagesList)
        }
    }
}
