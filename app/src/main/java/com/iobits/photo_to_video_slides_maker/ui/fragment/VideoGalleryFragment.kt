package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.iobits.photo_to_video_slides_maker.ui.dataModels.VideoDataClass
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentVideoGalleryBinding
import com.iobits.photo_to_video_slides_maker.managers.AdsManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.activities.ResultActivity
import com.iobits.photo_to_video_slides_maker.ui.adapters.VideoAdapter
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoGalleryFragment : Fragment() {
    val TAG = "VideoGalleryFragmentTag"
    val binding by lazy {
        FragmentVideoGalleryBinding.inflate(layoutInflater)
    }

    private val viewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private var mAdapter : VideoAdapter? = null
    private var videoList : ArrayList<VideoDataClass> = ArrayList()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
            initViews()
            fetchAllVideos()
            // Inflate the layout for this fragment
        return binding.root
    }
     private fun initViews(){
         binding.apply {
            next.setOnClickListener {
                if(dataShareViewModel.mVideoItem !=  null){
                    if(isAdded){
                        when(dataShareViewModel.tabNumber) {
                            5 ->{
                                dataShareViewModel.outputPath = dataShareViewModel.mVideoItem!!.path
                                try {
                                   // safeNavigate(R.id.action_videoGalleryFragment_to_resultFragment,R.id.videoGalleryFragment)
//                                    findNavController().navigate(R.id.to_resultFragment)
                                    val intent = Intent(requireContext() , ResultActivity::class.java)
                                    intent.putExtra("outputPath",dataShareViewModel.outputPath)
                                    intent.putExtra("tabNumber",dataShareViewModel.tabNumber)
                                    startActivity(intent)
                                }catch (e:Exception){
                                    Log.d(TAG, "initViews: ERROR ${e.localizedMessage}")
                                }
                            }
                            6 ->{
                                dataShareViewModel.outputPath = dataShareViewModel.mVideoItem!!.path
                                safeNavigate(R.id.action_videoGalleryFragment_to_videoCompressorFragment,R.id.videoGalleryFragment)
                            }
                            else -> {
                                safeNavigate(R.id.action_videoGalleryFragment_to_videoEditorFragment,R.id.videoGalleryFragment)
                            }
                        }
                    }
                }else{
                    Toast.makeText(requireContext(), "Please Select Video", Toast.LENGTH_SHORT).show()
                }
            }
             backBtn.setOnClickListener {
                 findNavController().navigateUp()
             }
         }
     }

    private fun fetchAllVideos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                mAdapter = VideoAdapter(requireContext())
                binding.videoRV.apply {
                    layoutManager = GridLayoutManager(requireContext(),3)
                    adapter = mAdapter
                    binding.progress.gone() }
                mAdapter?.onItemClick = {
                    dataShareViewModel.mVideoItem = videoList[it]
                }
                when(dataShareViewModel.tabNumber){
                    5 ->{
                        viewModel.apply {
                            try {
                                videoList = getVideosFromFolderPath(requireContext(),getFolderId(requireContext()))
                                mAdapter?.differ?.submitList(videoList)
                                if(videoList.isEmpty()){
                                    binding.placeholder.visible()
                                }else{
                                    binding.placeholder.gone()
                                    loadAds()
                                }
                            } catch (e:Exception){
                                Log.d(TAG, "fetchAllVideos: ${e.localizedMessage}")
                            }
                        }
                    }
                    else -> {
                        try {
                            viewModel.getAllVideosWithCount(requireContext()).collect {
                                if(it.isNotEmpty()){
                                    videoList.addAll(it)
                                    mAdapter?.differ?.submitList(videoList)
                                }
                                if(videoList.isEmpty()){
                                    binding.placeholder.visible()
                                }else{
                                    binding.placeholder.gone()
                                }
                            }
                        } catch (e:Exception)
                        {
                            Log.d(TAG, "getAllVideosWithCount: ${e.localizedMessage}")
                        }
                    }
                }
            }
        }
    }

    fun loadAds() {binding.adView.visible()
        MyApplication.mInstance.adsManager.loadNativeAd(
            requireActivity(),
            binding.adView,
            AdsManager.NativeAdType.NOMEDIA_MEDIUM,
            getString(R.string.ADMOB_NATIVE_WITH_MEDIA_V2),
            binding.shimmerLayout
        )
    }
}
