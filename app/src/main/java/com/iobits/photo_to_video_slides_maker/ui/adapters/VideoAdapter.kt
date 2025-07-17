package com.iobits.photo_to_video_slides_maker.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iobits.photo_to_video_slides_maker.ui.dataModels.VideoDataClass
import com.iobits.photo_to_video_slides_maker.databinding.LayoutVideoRvBinding


class VideoAdapter(
    private val context: Context,
) : RecyclerView.Adapter<VideoAdapter.MyHolder>() {

   private var selectorValue = -1
   var onItemClick : ((Int)-> Unit)? = null
    class MyHolder(val binding: LayoutVideoRvBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(LayoutVideoRvBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.binding.duration.text = DateUtils.formatElapsedTime(differ.currentList[position].duration/1000)
        if(selectorValue == position){

            holder.binding.radioChecked.visibility = View.VISIBLE

        } else {
            holder.binding.radioChecked.visibility = View.INVISIBLE
        }

        /**
         * Use glide to load Thumbnails in Rv
         */
        try {
            Glide.with(context)
                .asBitmap()
                .load(differ.currentList[position].artUri)
                .into(holder.binding.thumbNail)

        } catch (e: Exception) {
            e.localizedMessage
        }

        holder.binding.root.setOnClickListener {

            selectorValue = position
            notifyDataSetChanged()

            onItemClick?.invoke(position)
        }


    }

    private val differCallback = object: DiffUtil.ItemCallback<VideoDataClass>(){
        override fun areItemsTheSame(oldItem: VideoDataClass, newItem: VideoDataClass): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: VideoDataClass, newItem: VideoDataClass): Boolean {
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