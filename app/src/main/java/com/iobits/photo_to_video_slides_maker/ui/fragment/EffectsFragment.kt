package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentEffectsBinding
import com.iobits.photo_to_video_slides_maker.libraryUtils.widget.FilterItem
import com.iobits.photo_to_video_slides_maker.libraryUtils.widget.FilterType
import com.iobits.photo_to_video_slides_maker.ui.adapters.EffectImageAdapter
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate


class EffectsFragment : Fragment() {
    val binding by lazy {
        FragmentEffectsBinding.inflate(layoutInflater)
    }
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private var mAdapter: EffectImageAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        mAdapter = EffectImageAdapter(requireContext())
        initViews()
        return binding.root
    }
    private fun initViews(){
        binding.apply {
            check.setOnClickListener {
                safeNavigate(R.id.action_effectsFragment_to_sliderTabsFragment,R.id.effectsFragment)
            }
            cross.setOnClickListener {
                safeNavigate(R.id.action_effectsFragment_to_sliderTabsFragment,R.id.effectsFragment)
                dataShareViewModel.updateFilterValue(FilterItem(R.drawable.filter_default, "None", FilterType.NONE))
            }
            effectsRV.apply {
                adapter = mAdapter
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(),
                    LinearLayoutManager.HORIZONTAL,false)
            }
        }
        mAdapter?.updateList(dataShareViewModel.filterList)
        mAdapter?.onClick ={  dataShareViewModel.updateFilterValue(dataShareViewModel.filterList[it]) }
    }

}