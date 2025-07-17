package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdSize
import com.hw.photomovie.PhotoMovie
import com.hw.photomovie.PhotoMovieFactory
import com.hw.photomovie.PhotoMoviePlayer
import com.hw.photomovie.model.PhotoData
import com.hw.photomovie.model.PhotoSource
import com.hw.photomovie.model.SimplePhotoData
import com.hw.photomovie.render.GLSurfaceMovieRenderer
import com.hw.photomovie.render.GLTextureMovieRender
import com.hw.photomovie.render.GLTextureView
import com.hw.photomovie.timer.IMovieTimer
import com.hw.photomovie.util.MLog
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentEditorBinding
import com.iobits.photo_to_video_slides_maker.libraryUtils.IDemoView
import com.iobits.photo_to_video_slides_maker.libraryUtils.widget.FilterItem
import com.iobits.photo_to_video_slides_maker.managers.PreferenceManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ProgressServiceParams
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.utils.Constants
import com.iobits.photo_to_video_slides_maker.services.SlideShowProgressService
import com.iobits.photo_to_video_slides_maker.utils.disableMultipleClicking
import com.iobits.photo_to_video_slides_maker.utils.gone
import com.iobits.photo_to_video_slides_maker.utils.handleBackPress
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.utils.showExitDialogue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorFragment : Fragment(), IMovieTimer.MovieListener, IDemoView {

    val binding by lazy {
        FragmentEditorBinding.inflate(layoutInflater)
    }
    var isFullScreen = false
    private val mDemoView: IDemoView? = null
    private val TAG = "HomeFragmentTag";
    private val viewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    private var mPhotoMovie: PhotoMovie<*>? = null
    private var mPhotoMoviePlayer: PhotoMoviePlayer? = null
    private var mMovieRenderer: GLSurfaceMovieRenderer? = null
    private var mMovieType = PhotoMovieFactory.PhotoMovieType.HORIZONTAL_TRANS
    var isTransitionOnWaiting = false
    var outputFile:File? = null
    var name = ""
    var output = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initMoviePlayer()
        initSlider()
        initViews()
        loadAds()
        initListeners()
        // Inflate the layout for this fragment
        return binding.root
    }

    //-------------------------------------  LifeCycle methods -----------------------------------//
    override fun onPause() {
        super.onPause()
        if (mPhotoMoviePlayer?.isPlaying == true) {
            mPhotoMoviePlayer?.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        handleBackPress {
           moveBack()
        }
        if (mPhotoMoviePlayer?.isPlaying == false) {
            mPhotoMoviePlayer?.start()
        }
    }

    private fun moveBack(){
        showExitDialogue(requireActivity()){
        safeNavigate(R.id.action_editorFragment_to_dashboardFragment,R.id.editorFragment)

        }
    }

    //---------------------------------------- Initializers --------------------------------------//
    private fun initViews() {

        dataShareViewModel.editType = Constants.slideShow
        outputPath()
        dataShareViewModel.apply {
            onAddImageClick = {
                safeNavigate(R.id.action_editorFragment_to_galleryFragment, R.id.editorFragment)
            }
            onMusicTabClick = {
                safeNavigate(R.id.action_editorFragment_to_audioFragment, R.id.editorFragment)
            }
        }

        binding.apply {
            backBtn.setOnClickListener {
                safeNavigate(
                    R.id.action_editorFragment_to_dashboardFragment,
                    R.id.dashboardFragment
                )
            }
            playPause.setOnClickListener {
                if (mPhotoMoviePlayer?.isPlaying == true) {
                    mPhotoMoviePlayer?.pause()
                    binding.playPause.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.play
                        )
                    )
                } else {
                    mPhotoMoviePlayer?.start()
                    binding.playPause.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.pause
                        )
                    )
                }
            }
            fullScreen.setOnClickListener {
                isFullScreen = if (isFullScreen) {
                    guideline3.setGuidelinePercent(0.75f)
                    false
                } else {
                    guideline3.setGuidelinePercent(1f)
                    true
                }
            }
        }
    }
private fun loadAds(){
    MyApplication.mInstance.adsManager.showBanner(requireContext(), AdSize.BANNER,binding.adView,this.getString(R.string.ADMOB_BANNER_V2),binding.shimmerLayout);
}
    private fun initListeners() {
        binding.apply {
            backBtn.setOnClickListener {
                moveBack()
            }
            // Start the foreground service
            export.setOnClickListener {
                disableMultipleClicking(it)
                    try {
                        val params = ProgressServiceParams( requireContext(),
                            requireActivity(),
                            mPhotoMovie!!,
                            mPhotoMoviePlayer!!,
                            mMovieRenderer!!,
                            mMovieType,
                            glTexture,
                            dataShareViewModel.mMusicUri,output)
                        val intent = SlideShowProgressService.getStartIntent(requireContext(),params)
                        requireContext().startService(intent)

                        dataShareViewModel.isFromSlideShow = true
                        safeNavigate(R.id.action_editorFragment_to_exportFragment,R.id.editorFragment)
                    } catch (e: Exception) {
                        Log.d(TAG, "initListeners: ${e.localizedMessage}")
                    }
            }
        }
    }

    private fun initSlider() {
        Log.d(TAG, "displayListSize:${dataShareViewModel.selectedImagesList.size} ")
        if(dataShareViewModel.selectedImagesList.isNotEmpty()){
            lifecycleScope.launch(Dispatchers.IO) {
                val tempList: ArrayList<String?> = ArrayList()
                dataShareViewModel.selectedImagesList.forEach {
                    tempList.add(it.path)
                    Log.d(TAG, "List $tempList")
                    Log.d(TAG, "Size ${tempList.size}")
                }
                val photoDataList: MutableList<PhotoData> = ArrayList(tempList.size)
                for (path in tempList) {
                    val photoData: PhotoData =
                        SimplePhotoData(requireActivity(), path, PhotoData.STATE_LOCAL)
                    photoDataList.add(photoData)
                }
                withContext(Dispatchers.Main) {
                    if(photoDataList.size != 0){
                        onPhotoPick(photoDataList)
                        setMusic()
                    }
                }
            }
        }else{
            Toast.makeText(requireContext(), "Select Images", Toast.LENGTH_SHORT).show()
            moveBack()
        }

    }

    private fun outputPath() {
        val currentTimestamp = System.currentTimeMillis()
        name = "SlowMoMaker$currentTimestamp.mp4"

        var filePath: String = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val storageManager: StorageManager =
                requireActivity().getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager

            val storageVolume = storageManager.storageVolumes[0]

            val dir = File(storageVolume.directory!!.path + "/DCIM/SlowMoMaker")

            if (!dir.exists()) {
                dir.mkdirs()
            }
             outputFile = File(dir, name)
            filePath = outputFile!!.absolutePath

        } else {
            val outputDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val slowMoVideoDirectory = File(outputDirectory, "SlowMoMaker")
            if  (!slowMoVideoDirectory.exists()) {
                slowMoVideoDirectory.mkdirs()
            }
            filePath = "${slowMoVideoDirectory.absolutePath}/$name"
            output = filePath
        }
        output = filePath
        Log.d(TAG, "Output File is = $filePath")
    }
    private fun initMoviePlayer() {
        try {
            val glTextureView = binding.glTexture
            mMovieRenderer = GLTextureMovieRender(glTextureView)
            mPhotoMoviePlayer = PhotoMoviePlayer(requireContext())
            mPhotoMoviePlayer!!.setMovieRenderer(mMovieRenderer)
            mPhotoMoviePlayer!!.setMovieListener(this)
            mPhotoMoviePlayer!!.setLoop(true)
            mPhotoMoviePlayer!!.setOnPreparedListener(object : PhotoMoviePlayer.OnPreparedListener {
                override fun onPreparing(moviePlayer: PhotoMoviePlayer, progress: Float) {}
                override fun onPrepared(moviePlayer: PhotoMoviePlayer, prepared: Int, total: Int) {
                    requireActivity().runOnUiThread { mPhotoMoviePlayer!!.start() }
                }

                override fun onError(moviePlayer: PhotoMoviePlayer) {
                    MLog.i("onPrepare", "onPrepare error")
                }
            })
        } catch (e: Exception) {
            Log.d(TAG, "initMoviePlayer: ${e.localizedMessage}")
        }
    }

    //--------------------------------  Transition, Music , Effects ------------------------------//
    private fun dataObservers() {
        dataShareViewModel.apply {
            transitionValue.observe(viewLifecycleOwner, Observer {
                when (it) {
                    0 -> {
                        mMovieType = PhotoMovieFactory.PhotoMovieType.HORIZONTAL_TRANS
                    }

                    1 -> {
                        mMovieType = PhotoMovieFactory.PhotoMovieType.VERTICAL_TRANS
                    }

                    2 -> {
                        mMovieType = PhotoMovieFactory.PhotoMovieType.WINDOW
                    }

                    3 -> {
                        mMovieType = PhotoMovieFactory.PhotoMovieType.THAW
                    }

                    4 -> {
                        mMovieType = PhotoMovieFactory.PhotoMovieType.SCALE_TRANS
                    }

                    5 -> {
                        mMovieType = PhotoMovieFactory.PhotoMovieType.GRADIENT
                    }
                }
                if (mPhotoMoviePlayer?.isPlaying == false) {
                        playerWatcher()
                } else {
                    transitionSetter()
                }
            })
            filterValue.observe(viewLifecycleOwner, Observer {
                effectsSetter(it)
            })
        }
    }

    private fun playerWatcher(){
        val worker = Job()
        lifecycleScope.launch(Dispatchers.IO+worker){
            while(true){
                delay(500)
                withContext(Dispatchers.Main){
                    if(mPhotoMoviePlayer?.isPlaying==true){
                         transitionSetter()
                         worker.complete()
                         worker.cancel()
                    }
                }
            }
        }
    }

    private fun transitionSetter() {
        mPhotoMoviePlayer?.stop()
        mPhotoMovie = PhotoMovieFactory.generatePhotoMovie(mPhotoMovie?.photoSource, mMovieType)
        mPhotoMoviePlayer?.setDataSource(mPhotoMovie)
//        if (mMusicUri != null) {
//            mPhotoMoviePlayer!!.setMusic(requireActivity(), mMusicUri)
//        }
        Log.d(TAG, "transitionSetter: ${mPhotoMoviePlayer},${mPhotoMovie},${mMovieType}")
        mPhotoMoviePlayer?.setOnPreparedListener(object : PhotoMoviePlayer.OnPreparedListener {
            override fun onPreparing(moviePlayer: PhotoMoviePlayer, progress: Float) {}
            override fun onPrepared(moviePlayer: PhotoMoviePlayer, prepared: Int, total: Int) {
                requireActivity().runOnUiThread(Runnable { mPhotoMoviePlayer?.start() })
            }
            override fun onError(moviePlayer: PhotoMoviePlayer) {
                MLog.i("onPrepare", "onPrepare error")
            }
        })
        mPhotoMoviePlayer?.prepare()
    }

    private fun effectsSetter(filterItem: FilterItem) {
        mMovieRenderer?.movieFilter = filterItem.initFilter()
    }

    //------------------------------------------ Player ------------------------------------------//
    private fun startPlay(photoSource: PhotoSource) {
        mPhotoMovie = PhotoMovieFactory.generatePhotoMovie(photoSource, mMovieType)
        mPhotoMoviePlayer?.setDataSource(mPhotoMovie)
        mPhotoMoviePlayer?.prepare()

    }

    private fun onPhotoPick(photos: MutableList<PhotoData>) {
        val photoSource = PhotoSource(photos)
        if (mPhotoMoviePlayer == null) {
            startPlay(photoSource)
        }
        else {
            mPhotoMoviePlayer!!.stop()
            mPhotoMovie = PhotoMovieFactory.generatePhotoMovie(
                photoSource,
                mMovieType
            )
            mPhotoMoviePlayer!!.setDataSource(mPhotoMovie)
            if (dataShareViewModel.mMusicUri != null) {
                mPhotoMoviePlayer!!.setMusic(requireContext(), dataShareViewModel.mMusicUri)
            }
            mPhotoMoviePlayer!!.setOnPreparedListener(object : PhotoMoviePlayer.OnPreparedListener {
                override fun onPreparing(moviePlayer: PhotoMoviePlayer, progress: Float) {}
                override fun onPrepared(moviePlayer: PhotoMoviePlayer, prepared: Int, total: Int) {
                    requireActivity().runOnUiThread(Runnable { mPhotoMoviePlayer!!.start() })
                }

                override fun onError(moviePlayer: PhotoMoviePlayer) {
                    MLog.i("onPrepare", "onPrepare error")
                }
            })
            mPhotoMoviePlayer!!.prepare()
        }

        /** Transition Data Observer */
        dataObservers()
    }

    private fun setMusic() {
        if (dataShareViewModel.mMusicUri != null) {
            Log.d(TAG, "setMusic: Uri is ${dataShareViewModel.mMusicUri}")
            mPhotoMoviePlayer!!.setMusic(requireContext(), dataShareViewModel.mMusicUri)
        }
    }
    //----------------------------------------- Call backs ---------------------------------------//
    override fun onMovieUpdate(elapsedTime: Int) {

    }

    override fun onMovieStarted() {

    }

    override fun onMoviedPaused() {

    }

    override fun onMovieResumed() {

    }

    override fun onMovieEnd() {

    }

    override fun getGLView(): GLTextureView {
        return binding.glTexture
    }
}
