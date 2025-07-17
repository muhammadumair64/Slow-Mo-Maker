package com.iobits.photo_to_video_slides_maker.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iobits.photo_to_video_slides_maker.databinding.ItemImageGalleryBinding
import com.iobits.photo_to_video_slides_maker.databinding.LayoutItemTranstionsBinding
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass
import com.iobits.photo_to_video_slides_maker.ui.dataModels.TransitionDataClass


class TransitionImageAdapter(
    private val context: Context,
) : RecyclerView.Adapter<TransitionImageAdapter.MyHolder>() {
    private var mImageList: ArrayList<TransitionDataClass> = ArrayList()
    var onClick: ((Int) -> Unit)? = null
    class MyHolder(val binding: LayoutItemTranstionsBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.thumbNail
        val root = binding.root

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(LayoutItemTranstionsBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {

        /**
         * Use glide to load Thumbnails in Rv
         */
        try {
            Glide.with(context)
                .asBitmap()
                .load(mImageList[position].image)
                .into(holder.image)

        } catch (e: Exception) {
            e.localizedMessage
        }
        holder.binding.transitionName.text = mImageList[position].name
        holder.itemView.setOnClickListener {
            onClick?.invoke(position)
        }
    }
    fun updateList(mlist: List<TransitionDataClass>) {
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
