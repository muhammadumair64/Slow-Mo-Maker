package com.iobits.photo_to_video_slides_maker.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iobits.photo_to_video_slides_maker.databinding.MusicListLayoutBinding
import com.iobits.photo_to_video_slides_maker.ui.dataModels.AudioDataClass
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.invisible
import com.iobits.photo_to_video_slides_maker.utils.visible

class MusicListRvAdapter() : RecyclerView.Adapter<MusicListRvAdapter.MusicListViewHolder>() {

    private var musicListRvArrayList: ArrayList<AudioDataClass> = ArrayList()
    lateinit var context: Context
    var onClick: ((position: Int, isPlaying:Boolean) -> Unit)? = null
    private val TAG = "MUSIC_LIST_LISTENER"
    private var selectorValue = -1
    @SuppressLint("NotifyDataSetChanged")
    fun setUpAdapter(musicList: ArrayList<AudioDataClass>, activityContext: Context) {
        musicListRvArrayList = musicList
        context = activityContext
        notifyDataSetChanged()
    }

    class MusicListViewHolder(val bindingMusicList: MusicListLayoutBinding) :
        RecyclerView.ViewHolder(bindingMusicList.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicListViewHolder {
        val binding = MusicListLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicListViewHolder, @SuppressLint("RecyclerView") position: Int) {

        holder.bindingMusicList.audioTitle.text = musicListRvArrayList[position].title


        if(selectorValue == position) {
            holder.bindingMusicList.playAnim.visible()
            holder.bindingMusicList.playPause.invisible()
        } else {
            holder.bindingMusicList.playAnim.invisible()
            holder.bindingMusicList.playPause.visible()
        }

        holder.bindingMusicList.root.setOnClickListener {
            if( position == selectorValue ){
                selectorValue = -1
                onClick?.invoke(position , false )
                notifyDataSetChanged()
            }else{
                selectorValue = position
                notifyDataSetChanged()
                onClick?.invoke(position , true)
            }
        }
    }
    override fun getItemCount(): Int {
        return musicListRvArrayList.size
    }
}
