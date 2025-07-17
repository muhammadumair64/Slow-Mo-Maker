package com.iobits.photo_to_video_slides_maker.ui.viewModels

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.exoplayer.ExoPlayer
import com.iobits.photo_to_video_slides_maker.ui.dataModels.VideoDataClass
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.libraryUtils.widget.FilterItem
import com.iobits.photo_to_video_slides_maker.libraryUtils.widget.FilterType
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass
import com.iobits.photo_to_video_slides_maker.ui.dataModels.TransitionDataClass
import com.iobits.photo_to_video_slides_maker.ui.dataModels.VideoEditParams
import com.iobits.photo_to_video_slides_maker.utils.EditingOptionsValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DataShareViewModel @Inject constructor() : ViewModel() {

    /** Lists */
    var filterList: ArrayList<FilterItem> = ArrayList()
    var transitionList: ArrayList<TransitionDataClass> = ArrayList()
    var selectedImagesList: ArrayList<ImageDataClass> = ArrayList()
    val frames = HashSet<Bitmap>()


    /** Common params */
    var mMusicUri: Uri? = null
    var outputPath = ""
    var mVideoItem: VideoDataClass? = null
    var lastValueBeforeAudio = 0
    var isFromSlideShow = true
    var audioPath = ""
    var editType = ""
    var finalFileSize = ""


    /** Live data */
    var transitionValue = MutableLiveData<Int>()
    var filterValue = MutableLiveData<FilterItem>()

    /** Call backs */
    var hideExport:((Boolean)-> Unit)? =  null
    var onMusicTabClick: (() -> Unit)? = null
    var onAddImageClick: (() -> Unit)? = null
    var onMusicSelected: (() -> Unit)? = null
    var fullScreenCallBack: (() -> Unit)? = null
    var onEditOptionDoneClick: ((Int) -> Unit)? = null
    var clickOnReverse : (() -> Unit)? =  null
    /** video params */
    var miniDistance = 10000
    var trimStartingPoint = 0
    var trimEndingPoint = 0
    var player: ExoPlayer? = null
    var speedStartingPoint = 0
    var speedEndingPoint = 0
    var speedBarProgress = 50
    var params: VideoEditParams? = null
    var name = ""
    var isWithOutAudio = false
    var isFinalExport = false
    /** App Flow params */
    var tabNumber = 0
    var soloEditor = ""
    var isChangeApplied = false
    var isProcessingAudio = false

    init {
        initFilters()
        initTransitions()
    }

    private fun initFilters() {
        filterList.add(FilterItem(R.drawable.filter_default, "None", FilterType.NONE))
        filterList.add(FilterItem(R.drawable.gray, "BlackWhite", FilterType.GRAY))
        filterList.add(FilterItem(R.drawable.kuwahara, "Watercolour", FilterType.KUWAHARA))
        filterList.add(FilterItem(R.drawable.snow, "Snow", FilterType.SNOW))
        filterList.add(FilterItem(R.drawable.l1, "Lut_1", FilterType.LUT1))
        filterList.add(FilterItem(R.drawable.cameo, "Cameo", FilterType.CAMEO))
        filterList.add(FilterItem(R.drawable.l2, "Lut_2", FilterType.LUT2))
        filterList.add(FilterItem(R.drawable.l3, "Lut_3", FilterType.LUT3))
        filterList.add(FilterItem(R.drawable.l4, "Lut_4", FilterType.LUT4))
        filterList.add(FilterItem(R.drawable.l5, "Lut_5", FilterType.LUT5))
    }

    private fun initTransitions() {
        transitionList.add(TransitionDataClass(0, R.drawable.horizontal_trans, "HORIZONTA"))
        transitionList.add(TransitionDataClass(1, R.drawable.vertical_trans, "VERTICAL"))
        transitionList.add(TransitionDataClass(2, R.drawable.window, "WINDOW"))
        transitionList.add(TransitionDataClass(3, R.drawable.thow_trans, "THAW"))
        transitionList.add(TransitionDataClass(4, R.drawable.scale_trans, "SCALE_TRANS"))
        transitionList.add(TransitionDataClass(5, R.drawable.gradient_trans, "GRADIENT"))
    }

    /** Live Data Value Setter */
    fun updateTransitionValue(newValue: Int) {
        transitionValue.value = newValue
    }

    fun updateFilterValue(newValue: FilterItem) {
        filterValue.value = newValue
    }
    fun outputPathGenerator(requireActivity:Activity) {
        val currentTimestamp = System.currentTimeMillis()
       name = "SlowMoMaker$currentTimestamp.mp4"

        var filePath: String = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val storageManager: StorageManager =
                requireActivity.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager
            val storageVolume = storageManager.storageVolumes[0]
            val dir = File(requireActivity.cacheDir, "SlowMoMaker")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            filePath = File(dir,  name).absolutePath

        } else {
            val outputDirectory =
                requireActivity.cacheDir
            val slideShowMakerDirectory = File(outputDirectory, "SlowMoMaker")
            if (!slideShowMakerDirectory.exists()) {
                slideShowMakerDirectory.mkdirs()
            }
            filePath = "${slideShowMakerDirectory.absolutePath}/${name}"
        }
        outputPath = filePath
    }

    fun clearData() {
        frames.clear()
        params = null
        transitionValue.value = 0
        filterValue.value = filterList[0]
        selectedImagesList.clear()
        mMusicUri = null
        outputPath = ""
        soloEditor = ""
        tabNumber = 0
        isChangeApplied = false
        editType = ""
        name=""
        isWithOutAudio = false
        finalFileSize = ""
        mVideoItem = null
        isFinalExport = false
        EditingOptionsValidator.apply {
            commandMap.clear()
            isUsingOnlySpeed = false
            editorOptions.clear()
            isUsingOnlyTrimmer = false
        }
    }
}
