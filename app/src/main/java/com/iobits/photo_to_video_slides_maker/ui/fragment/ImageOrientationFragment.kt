package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentImageOrientationBinding
import com.iobits.photo_to_video_slides_maker.managers.AdsManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.adapters.ImageOrientationAdapter
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate

class ImageOrientationFragment : Fragment() {

    val binding by lazy {
        FragmentImageOrientationBinding.inflate(layoutInflater)
    }
    val TAG = "ImageOrientationFragmentTag"
    private val dataShareViewModel: DataShareViewModel by activityViewModels()

    private var mAdapter: ImageOrientationAdapter? = null
    private val itemTouchHelper by lazy {
        val itemTouchCallback = object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val recyclerviewAdapter = recyclerView.adapter as ImageOrientationAdapter
                val fromPosition = viewHolder.absoluteAdapterPosition
                val toPosition = target.absoluteAdapterPosition
                recyclerviewAdapter.moveItem(fromPosition, toPosition)
                //  recyclerviewAdapter.notifyItemMoved(fromPosition,toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                val updatedList = mAdapter?.differ?.currentList
                Log.d(TAG, "clearView: Updated List $updatedList")
            }
        }
        ItemTouchHelper(itemTouchCallback)
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mAdapter = ImageOrientationAdapter(requireContext())
        initListeners()
        loadAds()
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initListeners(){
        binding.apply {
            next.setOnClickListener {
                    dataShareViewModel.selectedImagesList.apply {
                        clear()
                        addAll(mAdapter?.orientedList!!)
                    }
                    if(dataShareViewModel.selectedImagesList.size > 2){
                        safeNavigate(R.id.action_imageOrientationFragment_to_editorFragment , R.id.imageOrientationFragment)
                    }else{
                        Toast.makeText(requireContext(), "Please Select Minimum 2 Images", Toast.LENGTH_SHORT).show()
                    }
          }
            backBtn.setOnClickListener { safeNavigate(R.id.action_imageOrientationFragment_to_galleryFragment,R.id.imageOrientationFragment) }

            imageRV.apply {
            layoutManager = GridLayoutManager(requireContext(),3)
            adapter = mAdapter
            itemTouchHelper.attachToRecyclerView(this)
            mAdapter!!.apply {
                differ.submitList(dataShareViewModel.selectedImagesList)
                setOrientedList(dataShareViewModel.selectedImagesList)
            }
          }
        }
        mAdapter?.onClick = { item: ImageDataClass ->
            dataShareViewModel.apply {
                mAdapter?.orientedList?.remove(item)
                dataShareViewModel.selectedImagesList.apply {
                    clear()
                    addAll(mAdapter?.orientedList!!)
                }
//                shareDataViewModel.selectedImagesList
                Log.d(TAG, "initListeners:${dataShareViewModel.selectedImagesList.size} || List: ${dataShareViewModel.selectedImagesList}")
                mAdapter!!.differ.submitList(selectedImagesList)
                mAdapter!!.notifyDataSetChanged()
            }
        }
    }

    fun loadAds(){
//        MyApplication.mInstance.adsManager.loadNativeAd(
//            requireActivity(),
//            binding.adView,
//            com.iobits.photo_to_video_slides_maker.managers.AdsManager.NativeAdType.NOMEDIA_MEDIUM,
//            getString(R.string.ADMOB_NATIVE_WITH_MEDIA_V2),
//            binding.shimmerLayout
//        )
    }
}
