package com.iobits.photo_to_video_slides_maker.ui.fragment

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.databinding.FragmentExportBinding
import com.iobits.photo_to_video_slides_maker.managers.AdsManager
import com.iobits.photo_to_video_slides_maker.managers.AnalyticsManager
import com.iobits.photo_to_video_slides_maker.myApplication.MyApplication
import com.iobits.photo_to_video_slides_maker.ui.viewModels.MainViewModel
import com.iobits.photo_to_video_slides_maker.ui.viewModels.DataShareViewModel
import com.iobits.photo_to_video_slides_maker.services.SlideShowProgressService
import com.iobits.photo_to_video_slides_maker.utils.handleBackPress
import com.iobits.photo_to_video_slides_maker.utils.safeNavigate
import com.iobits.photo_to_video_slides_maker.services.MusicProgressService
import com.iobits.photo_to_video_slides_maker.services.VideoEditorProgressService
import com.iobits.photo_to_video_slides_maker.ui.activities.ResultActivity
import com.iobits.photo_to_video_slides_maker.utils.EditingOptionsValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExportFragment : Fragment() {

    private var progressFlow = MutableStateFlow<Int?>(null)
    val binding by lazy {
        FragmentExportBinding.inflate(layoutInflater)
    }
    private val viewModel: MainViewModel by activityViewModels()
    private val dataShareViewModel: DataShareViewModel by activityViewModels()
    val TAG = "ExportFragmentTag"
    var name = ""
    var output = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initViews()
        loadAds()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        MyApplication.mInstance.isComingFromAction = true
        AnalyticsManager.logEvent("PROCESSING_VIDEO", null)
        if (dataShareViewModel.isProcessingAudio) {
            MusicProgressService.onComplete = {
                dataShareViewModel.mVideoItem?.artUri =
                    Uri.parse("file://${dataShareViewModel.outputPath}")
                safeNavigate(
                    R.id.action_exportFragment_to_videoEditorFragment,
                    R.id.exportFragment
                )
                dataShareViewModel.isProcessingAudio = false
            }
            binding.apply {
                title.text = "Processing Video"
                animationView10.apply {
                    setAnimation(R.raw.video_process)
                    loop(true)
                    playAnimation()
                }
            }
            progressFlow = MusicProgressService.progressFlow
        } else {
            if (dataShareViewModel.isFromSlideShow) {
                SlideShowProgressService.onComplete = { name: String, path: String ->

                    viewModel.updateMediaStore(
                        requireContext(),
                        path,
                        name,
                        completeListener,
                        progressListener
                    )

                    dataShareViewModel.outputPath = path
                }
                progressFlow = SlideShowProgressService.progressFlow
            } else {
                VideoEditorProgressService.onComplete = { name: String, path: String ->
                    Log.d(TAG, "initViews: On Complete for Trimmer $path")
                    if (EditingOptionsValidator.isUsingOnlySpeed || EditingOptionsValidator.isUsingOnlyTrimmer || EditingOptionsValidator.isUsingOnlyReverse) {
                        EditingOptionsValidator.apply {
                            isUsingOnlyReverse = false
                            isUsingOnlySpeed = false
                            isUsingOnlyTrimmer = false
                        }
                        outputPathGenerator()
                    } else {
                        dataShareViewModel.mVideoItem?.artUri =
                            Uri.parse("file://${dataShareViewModel.outputPath}")
                        safeNavigate(
                            R.id.action_exportFragment_to_videoEditorFragment,
                            R.id.exportFragment
                        )
                    }


                }
                progressFlow = VideoEditorProgressService.progressFlow
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                progressFlow.collect { progress ->
                    if (progress != null) {
                        binding.apply {
                            gradientSeekBar.value = progress.toFloat().div(100f)
                            if (progress <= 100) {
                                progressText.text = "${progress}% Complete"
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadAds() {
        MyApplication.mInstance.adsManager.loadNativeAd(
            requireActivity(),
            binding.adView,
            com.iobits.photo_to_video_slides_maker.managers.AdsManager.NativeAdType.NOMEDIA_MEDIUM,
            getString(R.string.ADMOB_NATIVE_WITH_MEDIA_V2),
            binding.shimmerLayout
        )
    }


    override fun onResume() {
        super.onResume()
        handleBackPress {
//            moveBack()
        }
        if (!isServiceRunning(
                requireContext(),
                VideoEditorProgressService::class.java
            ) && !isServiceRunning(
                requireContext(),
                SlideShowProgressService::class.java
            ) && !isServiceRunning(requireContext(), MusicProgressService::class.java)
        ) {
            moveBack()
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        val services = manager?.getRunningServices(Int.MAX_VALUE)

        if (services != null) {
            for (service in services) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }

    private fun moveBack() {
        safeNavigate(R.id.action_exportFragment_to_dashboardFragment, R.id.editorFragment)
    }

    private fun outputPathGenerator() {
        Log.d(TAG, "outputPathGenerator: begin")
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
            filePath = File(dir, name).absolutePath

        } else {
            val outputDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val slowMoVideoDirectory = File(outputDirectory, "SlowMoMaker")
            if (!slowMoVideoDirectory.exists()) {
                slowMoVideoDirectory.mkdirs()
            }
            filePath = "${slowMoVideoDirectory.absolutePath}/$name"
            output = filePath
        }
        Log.d(TAG, "outputPathGenerator: new file path created")

        // Move file from cache to generated path
        val cacheFile = File(dataShareViewModel.outputPath)
        val outputFile = File(filePath)
        cacheFile.copyTo(outputFile, overwrite = true)

        output = filePath
        dataShareViewModel.outputPath = output
        Log.d(TAG, "before clear cache path")
        viewModel.clearCacheDirectory(requireActivity().cacheDir)
        Log.d(TAG, "after clear cache path")
        lifecycleScope.launch(Dispatchers.IO) {
            delay(2000)
            withContext(Dispatchers.Main) {
                Log.d(TAG, "before media store")
                viewModel.updateMediaStore(
                    requireContext(),
                    filePath,
                    name,
                    completeListener,
                    progressListener
                )
            }

        }
    }

    private val completeListener =
        MediaScannerConnection.OnScanCompletedListener { path, uri -> // Handle completion of scanning here
            Log.d(TAG, "Scan completed for $path")
            if (isAdded) {
                try {
//                    safeNavigate(
//                        R.id.action_exportFragment_to_resultFragment,
//                        R.id.exportFragment
//                    )
                   // findNavController().navigate(R.id.to_resultFragment)
                    val intent = Intent(requireContext() , ResultActivity::class.java)
                    intent.putExtra("outputPath",dataShareViewModel.outputPath)
                    intent.putExtra("tabNumber",dataShareViewModel.tabNumber)
                    startActivity(intent)
                }catch (e:Exception){
                    Log.d(TAG, "ERROR  ${e.localizedMessage}")
                }
            }
        }

    private val progressListener =
        MediaScannerConnection.OnScanCompletedListener { path, uri -> // Handle progress of scanning here
            Log.d(TAG, "Scanning progress for $path")
        }
}
