package com.iobits.photo_to_video_slides_maker.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iobits.photo_to_video_slides_maker.databinding.LayoutImageRvBinding
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.visible


class ImageAdapter(
    private val context: Context,
    private var isFolder: Boolean = false,
    val  recyclerView: RecyclerView
) : RecyclerView.Adapter<ImageAdapter.MyHolder>() {

    var onClick: ((list: ArrayList<ImageDataClass>, position: Int) -> Unit)? = null
    var tempList = ArrayList<ImageDataClass>()
    val TAG = "ImageAdapterTag"

    class MyHolder(val binding: LayoutImageRvBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(LayoutImageRvBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {

        /**
         * Use glide to load Thumbnails in Rv
         */

        if (tempList.contains(differ.currentList[position])) {
            holder.binding.radioChecked.visible()
        } else {
            holder.binding.radioChecked.gone()
        }

        try {
            Glide.with(context)
                .asBitmap()
                .load(differ.currentList[position].artUri)
                .into(holder.binding.thumbNail)
        } catch (e: Exception) {
            e.localizedMessage
        }

        holder.binding.root.setOnClickListener {
            if (holder.binding.radioChecked.isVisible) {
                Log.d(TAG, "onBindViewHolder: unChecked")
                tempList.remove(differ.currentList[position])
                onClick?.invoke(tempList, position)
                holder.binding.apply {
                    radioChecked.gone()
                }
            } else {
                Log.d(TAG, "onBindViewHolder: Checked")
                tempList.add(differ.currentList[position])
                onClick?.invoke(tempList, position)
                holder.binding.apply {
                    radioChecked.visible()
                }
            }
        }
    }

    private val differCallback = object: DiffUtil.ItemCallback<ImageDataClass>(){
        override fun areItemsTheSame(oldItem: ImageDataClass, newItem: ImageDataClass): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ImageDataClass, newItem: ImageDataClass): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val list = differ.currentList.toMutableList()
        val fromItem = list[fromPosition]
        list.removeAt(fromPosition)
        if (toPosition < fromPosition) {
            list.add(toPosition + 1 , fromItem)
        } else {
            list.add(toPosition - 1, fromItem)
        }
        differ.submitList(list)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}
