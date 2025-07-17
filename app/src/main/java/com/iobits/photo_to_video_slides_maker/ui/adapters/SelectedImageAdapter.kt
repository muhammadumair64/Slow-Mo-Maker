package com.iobits.photo_to_video_slides_maker.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iobits.photo_to_video_slides_maker.databinding.ItemImageGalleryBinding
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass


class SelectedImageAdapter(
    private val context: Context,
    private var isFolder: Boolean = false,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<SelectedImageAdapter.MyHolder>() {
    private var mImageList: ArrayList<ImageDataClass> = ArrayList()
    var onClick: ((Int) -> Unit)? = null
    class MyHolder(val binding: ItemImageGalleryBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.thumbNail
        val root = binding.root

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemImageGalleryBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        /**
         * Use glide to load Thumbnails in Rv
         */
        try {
            Glide.with(context)
                .asBitmap()
                .load(mImageList[position].artUri)
                .into(holder.image)

        } catch (e: Exception) {
            e.localizedMessage
        }

        holder.binding.cross.setOnClickListener {
            onClick?.invoke(position)
        }
    }
    fun updateList(mlist: List<ImageDataClass>) {
        mImageList.clear()
        mImageList.addAll(mlist)
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return mImageList.size
    }
    override fun getItemViewType(position: Int): Int {
        return position
    }
}
