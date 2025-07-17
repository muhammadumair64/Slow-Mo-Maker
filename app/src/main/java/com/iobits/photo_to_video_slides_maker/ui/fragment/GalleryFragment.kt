package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentGalleryBinding
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.adapters.ImageAdapter
import com.iobits.photo_to_video_slides_maker.ui.adapters.SelectedImageAdapter
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.disableMultipleClicking
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.handleBackPress
import com.iobits.photo_to_video_slides_maker.utils.popBackStack
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.utils.showToast
import com.iobits.photo_to_video_slides_maker.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GalleryFragment : Fragment() {
    private var mAdapter: ImageAdapter? = null
    private val TAG = "GalleryFragmentTag"
    private val binding by lazy {
        FragmentGalleryBinding.inflate(layoutInflater)
    }
    private var imagesList : ArrayList<ImageDataClass> = ArrayList()
    private val viewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private var selectedImageAdapter: SelectedImageAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fetchAllImages()
        binding.backBtn.setOnClickListener {
            moveBack()
        }
        return binding.root
    }

    private fun fetchAllImages() {
        lifecycleScope.launch {
            Log.d(TAG, "fetchAllImages: LIST Size ${imagesList.size}")
                 mAdapter = ImageAdapter(requireContext(), false, binding.imageRV)
                  selectedImageAdapter = SelectedImageAdapter(requireContext(),false,binding.selectedImageRV)
                  binding.imageRV.apply {
                      layoutManager = GridLayoutManager(requireContext(),3)
                      adapter = mAdapter
                      binding.progress.gone()
                     //   itemTouchHelper.attachToRecyclerView(binding.imageRV)
                    //  mAdapter!!.differ.submitList(imagesList)
                   //          mAdapter!!.updateList(imagesList)
                  }
                selectedImageRV()
                initListeners()
            viewModel.getAllImagesWithCount(requireContext()).collect {
                imagesList.addAll(it)
                mAdapter!!.differ.submitList(imagesList)
                if(imagesList.isEmpty()){
                    binding.placeholder.visible()
                }else{
                    binding.placeholder.gone()
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        handleBackPress {
            moveBack()
        }
    }
    private fun moveBack() {
       popBackStack()
    }
    @SuppressLint("SuspiciousIndentation", "NotifyDataSetChanged")
    private fun initListeners(){
        mAdapter?.onClick = { list: ArrayList<ImageDataClass>, position: Int ->
            dataShareViewModel.apply {
                 selectedImagesList.clear()
                 selectedImagesList.addAll(list)
                 selectedImageAdapter?.updateList(selectedImagesList)
                Log.d(TAG, "initListeners: MY LIST IS ${selectedImagesList.size}")
            }
            if(dataShareViewModel.selectedImagesList.size > 2 ){
                binding.selectedImageRV.smoothScrollToPosition(dataShareViewModel.selectedImagesList.size-1)
            }
        }

        selectedImageAdapter?.onClick = { position: Int ->
            dataShareViewModel.apply {
                try {
                    selectedImagesList.removeAt(position)
                    selectedImageAdapter?.updateList(selectedImagesList)
                    mAdapter?.apply {
                        tempList.clear()
                        tempList.addAll(selectedImagesList)
                        mAdapter!!.notifyDataSetChanged()
                    }
                    Log.d(TAG, "initListeners: MY LIST IS ${selectedImagesList.size}")
                }catch (e:Exception){
                    e.localizedMessage
                }
            }
        }
        binding.next.setOnClickListener {
                if(dataShareViewModel.selectedImagesList.size <= 2){
                    disableMultipleClicking(binding.next)
                    showToast("Please Select Minimum 3 Images")
                }else{
                    safeNavigate(R.id.action_galleryFragment_to_imageOrientationFragment ,R.id.galleryFragment )
                }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun selectedImageRV(){
        binding.selectedImageRV.apply{
            layoutManager= LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL ,false)
            adapter = selectedImageAdapter
        }
         selectedImageAdapter?.updateList(dataShareViewModel.selectedImagesList)
         mAdapter?.tempList?.addAll(dataShareViewModel.selectedImagesList)
         mAdapter!!.notifyDataSetChanged()
    }
}
