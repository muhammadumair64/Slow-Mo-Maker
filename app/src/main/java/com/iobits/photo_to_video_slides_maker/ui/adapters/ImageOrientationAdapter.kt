package com.iobits.photo_to_video_slides_maker.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iobits.photo_to_video_slides_maker.databinding.ItemImageOrientaionBinding
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass


class ImageOrientationAdapter(
    private val context: Context,
) : RecyclerView.Adapter<ImageOrientationAdapter.MyHolder>() {
    var onClick: ((ImageDataClass) -> Unit)? = null
    var orientedList :ArrayList<ImageDataClass> = ArrayList()
    val TAG = "ImageOrientationAdapterTag"
    class MyHolder(val binding: ItemImageOrientaionBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.thumbNail
        val root = binding.root

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemImageOrientaionBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        /**
         * Use glide to load Thumbnails in Rv
         */
        try {
            Glide.with(context)
                .asBitmap()
                .load(differ.currentList[position].artUri)
                .into(holder.image)

        } catch (e: Exception) {
            e.localizedMessage
        }

        holder.binding.cross.setOnClickListener {
            onClick?.invoke(differ.currentList[position])
        }
    }

    private val differCallback = object: DiffUtil.ItemCallback<ImageDataClass>() {
        override fun areItemsTheSame(oldItem: ImageDataClass, newItem: ImageDataClass): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ImageDataClass, newItem: ImageDataClass): Boolean {
            return oldItem == newItem
        }
    }

    fun setOrientedList(list: MutableList<ImageDataClass>) {
        orientedList.clear()
        orientedList.addAll(list)
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
        setOrientedList(differ.currentList)
        Log.d(TAG, "moveItem: List ${differ.currentList}")

    }
    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}
