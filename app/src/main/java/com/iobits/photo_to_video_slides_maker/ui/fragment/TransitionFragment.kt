package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentTransitionBinding
import com.iobits.photo_to_video_slides_maker.ui.adapters.TransitionImageAdapter
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate

class TransitionFragment : Fragment() {
    val binding by lazy {
        FragmentTransitionBinding.inflate(layoutInflater)
    }
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private var mAdapter: TransitionImageAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mAdapter = TransitionImageAdapter(requireContext())
        initViews()
        return binding.root
    }

    private fun initViews(){
        binding.apply {
            check.setOnClickListener {
                safeNavigate(R.id.action_transitionFragment_to_sliderTabsFragment,R.id.transitionFragment)
            }
            cross.setOnClickListener {
                safeNavigate(R.id.action_transitionFragment_to_sliderTabsFragment,R.id.transitionFragment)
                dataShareViewModel.updateTransitionValue(0)
            }
            transitionsRV.apply {
                adapter = mAdapter
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
            }
        }
        mAdapter?.updateList(dataShareViewModel.transitionList)
        mAdapter?.onClick ={
            dataShareViewModel.updateTransitionValue(dataShareViewModel.transitionList[it].id)
        }
    }
}
