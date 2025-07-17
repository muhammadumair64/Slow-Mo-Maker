package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.recyclerview.widget.LinearLayoutManager
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentAudioBinding
import com.iobits.photo_to_video_slides_maker.ui.adapters.MusicListRvAdapter
import com.iobits.photo_to_video_slides_maker.ui.dataModels.AudioDataClass
import com.iobits.photo_to_video_slides_maker.ui.dataModels.MusicEditParams
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.Constants
import com.iobits.photo_to_video_slides_maker.utils.handleBackPress
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.services.MusicProgressService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AudioFragment : Fragment() {

    private val binding by lazy {
        FragmentAudioBinding.inflate(layoutInflater)
    }
    private var mAdapter: MusicListRvAdapter? = null
    private val viewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    var selectedAudio: AudioDataClass? = null
    val TAG = "AudioFragmentTag"
    private var player: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fetchAudios()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        handleBackPress {
            if(dataShareViewModel.editType == Constants.video){
                safeNavigate(R.id.action_audioFragment_to_videoEditorFragment, R.id.audioFragment)
            } else {
                safeNavigate(R.id.action_audioFragment_to_editorFragment, R.id.audioFragment)
            }
        }
        try {
            if(!player!!.isPlaying){
                player!!.play()
            }
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if(player!!.isPlaying){
                player!!.release()
            }
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    private fun fetchAudios() {
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d(TAG, "setAudios: Init")
            if(viewModel.audioList.isEmpty()){
                try {
                    viewModel.audioList.apply {
                        addAll(viewModel.getAllExternalAudioFiles(requireContext()))
                        addAll(viewModel.getAllInternalAudioFiles(requireContext()))
                    }
                }catch (e:Exception){
                    Log.d(TAG, "fetchAudios: ERROR ${e.localizedMessage}")
                }
            }
            Log.d(TAG, "setAudios:${viewModel.audioList}")
            mAdapter = MusicListRvAdapter()
            withContext(Dispatchers.Main) {
                binding.progress.visibility = View.GONE
                mAdapter?.setUpAdapter(viewModel.audioList, requireContext())
                binding.audiosRv.apply {
                    this.adapter = mAdapter
                    layoutManager = LinearLayoutManager(context)
                }
                initListeners()
            }
        }
    }

    private fun initListeners() {
        binding.imgBack.setOnClickListener {
            if(dataShareViewModel.editType == Constants.video){
                safeNavigate(R.id.action_audioFragment_to_videoEditorFragment, R.id.audioFragment)
            } else {
                safeNavigate(R.id.action_audioFragment_to_editorFragment, R.id.audioFragment)
            }
        }
        mAdapter?.onClick = { position: Int, isPlaying: Boolean ->
            viewModel.apply {
                selectedAudio = audioList[position]
                dataShareViewModel.mMusicUri = selectedAudio!!.artUri
            }
            if (isPlaying) {
                playAudio(position)
            } else {
                try {
                    if (player!!.isPlaying) {
                        player!!.release()
                    }
                } catch (e: Exception) {
                    e.localizedMessage
                }
            }
        }

        binding.apply {
            check.setOnClickListener {
                moveNext()
            }
        }
    }

    private fun moveNext() {
        if(dataShareViewModel.editType == Constants.video){
            processMusic()
        } else {
            safeNavigate(R.id.action_audioFragment_to_editorFragment, R.id.audioFragment)
        }
        dataShareViewModel.onMusicSelected?.invoke()
        if (player != null) {
            try {
                if (player!!.isPlaying) {
                    player!!.release()
                }
            } catch (e: Exception) {
                e.localizedMessage
            }
        }
    }

    private fun processMusic() {
        lifecycleScope.launch {
            if(dataShareViewModel.mMusicUri != null){
                try {
                dataShareViewModel.outputPathGenerator(requireActivity())
                val fInput = FFmpegKitConfig.getSafParameterForRead(
                    requireContext(),
                    dataShareViewModel.mVideoItem?.artUri
                )
                val fAudioInput = FFmpegKitConfig.getSafParameterForRead(
                    requireContext(),
                    dataShareViewModel.mMusicUri
                )
                val cmd = "-y -i $fInput -stream_loop -1 -i $fAudioInput -filter_complex \"[1:a]apad=whole_dur=1000000[a1];[a1][1:a]concat=n=2:v=0:a=1[a]\" -map 0:v:0 -map \"[a]\" -c:v mpeg4 -b:v 8192k -preset superfast -c:v copy -c:a aac -shortest ${dataShareViewModel.outputPath}"
                val mParams = MusicEditParams(cmd, viewModel.duration)
                    val intent = MusicProgressService.getStartIntent(
                    requireContext(),
                    mParams
                )
                    requireContext().startService(intent)
                dataShareViewModel.isProcessingAudio = true
                    dataShareViewModel.isChangeApplied = true
                safeNavigate(R.id.action_audioFragment_to_exportFragment, R.id.audioFragment)
                } catch (e:Exception){
                    e.localizedMessage
                }
            }
        }
    }

    @OptIn(UnstableApi::class) private fun playAudio(position: Int) {
        // Create the ExoPlayer instance
        player?.release()
        player = ExoPlayer.Builder(requireContext()).build()
        // Create a media source
        val audioUri = Uri.parse(viewModel.audioList[position].artUri.toString()) // Replace with the URI of your audio file
        val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext(), "user-agent")) .createMediaSource(MediaItem.fromUri(audioUri))
        // Enable auto-looping
        player?.repeatMode = Player.REPEAT_MODE_ALL
        // Prepare the player with the media source
        player?.prepare(mediaSource)
        // Start playback
        player?.playWhenReady = true
    }
}
