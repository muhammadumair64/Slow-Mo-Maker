package com.iobits.photo_to_video_slides_maker.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iobits.photo_to_video_slides_maker.databinding.ItemImageGalleryBinding
import com.iobits.photo_to_video_slides_maker.databinding.LayoutItemEffectsBinding
import com.iobits.photo_to_video_slides_maker.databinding.LayoutItemTranstionsBinding
import com.iobits.photo_to_video_slides_maker.libraryUtils.widget.FilterItem
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass
import com.iobits.photo_to_video_slides_maker.ui.dataModels.TransitionDataClass


class EffectImageAdapter(
    private val context: Context,
) : RecyclerView.Adapter<EffectImageAdapter.MyHolder>() {
    private var mImageList: ArrayList<FilterItem> = ArrayList()
    var onClick: ((Int) -> Unit)? = null
    class MyHolder(val binding: LayoutItemEffectsBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.thumbNail
        val root = binding.root

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(LayoutItemEffectsBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {

        /** Use glide to load Thumbnails in Rv */
        try {
            Glide.with(context)
                .asBitmap()
                .load(mImageList[position].imgRes)
                .into(holder.image)
        } catch (e: Exception) {
            e.localizedMessage
        }
        holder.binding.effectName.text = mImageList[position].name
        holder.itemView.setOnClickListener {
            onClick?.invoke(position)
        }
    }
    fun updateList(mlist: List<FilterItem>) {
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
