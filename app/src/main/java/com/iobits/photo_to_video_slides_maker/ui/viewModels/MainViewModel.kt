package com.iobits.photo_to_video_slides_maker.ui.viewModels

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.iobits.photo_to_video_slides_maker.ui.dataModels.VideoDataClass
import com.iobits.photo_to_video_slides_maker.ui.dataModels.AudioDataClass
import com.iobits.photo_to_video_slides_maker.ui.dataModels.FolderDataClass
import com.iobits.photo_to_video_slides_maker.ui.dataModels.ImageDataClass
import com.iobits.photo_to_video_slides_maker.utils.VideoFrameView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private var folderList: ArrayList<FolderDataClass> = ArrayList()
    var audioList: ArrayList<AudioDataClass> = ArrayList()
    var folderDataClassList: ArrayList<FolderDataClass> = ArrayList()
    val TAG = "MainViewModelTag"

    var duration = 0
    var speedUnit = 1f
    var speedForFFmpeg = 1f
    var frameCallback: ((Int) -> Unit)? = null

    //-------------------------------------- Media Fetchers --------------------------------------//

    fun loadAudios(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (audioList.isEmpty()) {
                launch {
                    try {
                        audioList.addAll(getAllInternalAudioFiles(context))
                    }catch (e:Exception){
                        Log.d(TAG, "loadAudios: ERROR ${e.localizedMessage}")}
                }
                launch {
                    try {
                        audioList.addAll(getAllExternalAudioFiles(context))
                    }catch (e:Exception){
                        Log.d(TAG, "loadAudios: ERROR ${e.localizedMessage}")}
                }
                audioList.sortBy {
                    it.title
                }
            }
            else{
                var internalList = ArrayList<AudioDataClass>()
                var externalList = ArrayList<AudioDataClass>()
                launch {
                    try {
                        internalList = getAllInternalAudioFiles(context)
                    } catch (e:Exception) {
                        Log.d(TAG, "loadAudios: ERROR ${e.localizedMessage}")}
                }
                launch {
                    try {
                        externalList = getAllExternalAudioFiles(context)
                    }catch (e:Exception){
                        Log.d(TAG, "loadAudios: ERROR ${e.localizedMessage}")}
                }
                externalList.forEach {
                    if(!audioList.contains(it)){
                        audioList.add(it)
                    }
                }
                internalList.forEach {
                    if(!audioList.contains(it)){
                        audioList.add(it)
                    }
                }
                audioList.sortBy {
                    it.title
                }

            }
        }
    }

    /** Images */
    @SuppressLint("Range")
    fun getAllImagesWithCount(context: Context): Flow<List<ImageDataClass>> = flow {
        val tempFolderList = ArrayList<String>()
        val folderList =
            mutableListOf<FolderDataClass>()  // Assuming this is a local list for demonstration

        val projection = arrayOf(
            MediaStore.Images.Media.TITLE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        val batchSize = 10
        var tempList = ArrayList<ImageDataClass>()

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val title = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.TITLE))
                val id = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID))
                val folderName =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                val size = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.SIZE))
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))

                try {
                    val file = File(path)
                    val artUri = Uri.fromFile(file)
                    val image = ImageDataClass(
                        title = title,
                        id = id,
                        folderName = folderName,
                        size = size,
                        path = path,
                        artUri = artUri
                    )
                    if (file.exists()) {
                        tempList.add(image)
                    }
                    // For adding folders
                    if (!tempFolderList.contains(folderName) && !folderName.contains("Internal Storage")) {
                        tempFolderList.add(folderName)
                        val folderDataClass = FolderDataClass(id, folderName, 1, path)
                        folderList.add(folderDataClass)
                    } else {
                        // Increment the image count for the existing folder
                        val index = folderList.indexOfFirst { it.folderName == folderName }
                        folderList[index].itemCount += 1
                    }

                } catch (e: Exception) {
                    Log.d("ImageFetchError", "ERROR ${e.localizedMessage}")
                }

                if (tempList.size == batchSize) {
                    emit(tempList)
                    tempList = ArrayList()  // Reset the list for the next batch
                }
            } while (cursor.moveToNext())

            // Emit any remaining images that didn't fill a full batch
            if (tempList.isNotEmpty()) {
                emit(tempList)
            }
        }
        cursor?.close()
    }.flowOn(Dispatchers.IO)

    /** Audios */
    @SuppressLint("Range")
    fun getAllExternalAudioFiles(context: Context): ArrayList<AudioDataClass> {

        val tempList = ArrayList<AudioDataClass>()
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder
        )

        cursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                val size = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                val duration =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                        ?.toLong() ?: 0L
                try {
                    val file = File(path)
                    val albumArtBitmap = extractEmbeddedAlbumArt(path, 100, 100)
                    val audio = AudioDataClass(
                        title = title,
                        id = id,
                        album = album,
                        duration = duration,
                        size = size,
                        path = path,
                        artUri = Uri.fromFile(file),
                        albumArtPath = albumArtBitmap
                    )
                    if (file.exists()) {
                        tempList.add(audio)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        cursor?.close()
        return tempList
    }

    @SuppressLint("Range")
    fun getAllInternalAudioFiles(context: Context): ArrayList<AudioDataClass> {
        val tempList = ArrayList<AudioDataClass>()
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = MediaStore.Audio.Media.DATA + " like ?"
        val selectionArgs = arrayOf("%")
        val sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val titleC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                    val albumC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    val albumIdC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val durationC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                            ?.toLong() ?: 0L

                    try {
                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val albumArtBitmap = extractEmbeddedAlbumArt(pathC, 100, 100)
                        val audio = AudioDataClass(
                            title = titleC,
                            id = idC,
                            album = albumC,
                            duration = durationC,
                            size = sizeC,
                            path = pathC,
                            artUri = artUriC,
                            albumArtBitmap
                        )
                        if (file.exists()) {
                            tempList.add(audio)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } while (cursor.moveToNext())
            }
        }

        cursor?.close()
        return tempList
    }

    private fun extractEmbeddedAlbumArt(audioFilePath: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audioFilePath)
            val embeddedArt = retriever.embeddedPicture
            embeddedArt?.let {
                // Decode the byte array into a Bitmap
                val originalBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                // Scale the bitmap to the desired width and height
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }


    /** Videos */
    @SuppressLint("Range")
    fun getAllVideosWithCount(context: Context): Flow<List<VideoDataClass>> = flow {
        val tempFolderList = ArrayList<String>()
        val folderDataClassList =
            mutableListOf<FolderDataClass>()  // Assuming this is a local list for demonstration

        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )

        val batchSize = 10
        var tempList = ArrayList<VideoDataClass>()

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                val folderC =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                val folderIdC =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))
                val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                val durationC =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                        ?.toLong() ?: 0L

                try {
                    val file = File(pathC)

                    val artUriC = Uri.fromFile(file)
                    val video = VideoDataClass(
                        title = titleC,
                        id = idC,
                        folderName = folderC,
                        duration = durationC,
                        size = sizeC,
                        path = pathC,
                        artUri = artUriC
                    )
                    if (file.exists()) {
                        tempList.add(video)
                    }
                    // For adding folders
                    if (!tempFolderList.contains(folderC) && !folderC.contains("Internal Storage")) {
                        tempFolderList.add(folderC)
                        val folderDataClass = FolderDataClass(folderIdC, folderC, 1, pathC)
                        folderDataClassList.add(folderDataClass)
                    } else {
                        // Increment the video count for the existing folder
                        val index = folderDataClassList.indexOfFirst { it.folderName == folderC }
                        folderDataClassList[index].itemCount += 1
                    }

                } catch (e: Exception) {
                    Log.d("VideoFetchError", "ERROR: ${e.localizedMessage}")
                }

                if (tempList.size == batchSize) {
                    emit(tempList)
                    tempList = ArrayList()  // Reset the list for the next batch
                }
            }
            // Emit any remaining videos that didn't fill a full batch
            if (tempList.isNotEmpty()) {
                emit(tempList)
            } else {
                emit(emptyList<VideoDataClass>())
            }
        }

        cursor?.close()
    }.flowOn(Dispatchers.IO)

    @SuppressLint("Range")
    fun getFolderVideos(folderId: String, context: Context): ArrayList<VideoDataClass> {
        Log.d("GalleryViewModel", "folder ID = $folderId")
        val tempList = ArrayList<VideoDataClass>()
        val selection = MediaStore.Video.Media.BUCKET_ID + " like? "
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(folderId),
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )
        if (cursor != null)
            if (cursor.moveToNext())
                do {
                    try {

                        val titleC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                        val idC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                        val folderC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                        val sizeC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                        val pathC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                        val durationC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                                .toLong()


                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val video = VideoDataClass(
                            title = titleC,
                            id = idC,
                            folderName = folderC,
                            duration = durationC,
                            size = sizeC,
                            path = pathC,
                            artUri = artUriC
                        )
                        if (file.exists()) tempList.add(video)

                    } catch (e: Exception) {
                        e.localizedMessage
                    }
                } while (cursor.moveToNext())
        cursor?.close()
        return tempList
    }

    @SuppressLint("Range")
    fun getVideosFromFolderPath(context: Context, folderPath: String): ArrayList<VideoDataClass> {
        val tempList = ArrayList<VideoDataClass>()
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )

        val selection = MediaStore.Video.Media.DATA + " LIKE? "
        val selectionArgs = arrayOf("$folderPath/%")
        val sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                val folderC =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                val durationC =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                        ?.toLong() ?: 0L

                try {
                    val file = File(pathC)
                    val artUriC = Uri.fromFile(file)
                    val video = VideoDataClass(
                        title = titleC,
                        id = idC,
                        folderName = folderC,
                        duration = durationC,
                        size = sizeC,
                        path = pathC,
                        artUri = artUriC
                    )

                    if (file.exists()) tempList.add(video)
                } catch (e: Exception) {
                    e.localizedMessage
                }
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return tempList
    }

    fun getFolderId(context: Context): String {
        val folderPath: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val storageManager: StorageManager =
                context.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager
            val storageVolume = storageManager.storageVolumes[0]

            val dir = File(storageVolume.directory!!.path + "/DCIM/SlowMoMaker")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            folderPath = dir.toString()
            Log.d(TAG, "folderPath ${folderPath.trim()}")
            return folderPath
        } else {
            val outputDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val slowMoVideoDirectory = File(outputDirectory, "SlowMoMaker")
            if (!slowMoVideoDirectory.exists()) {
                slowMoVideoDirectory.mkdirs()
            }
            folderPath = slowMoVideoDirectory.toString()
            Log.d(TAG, "folderPath ${folderPath.trim()}")

            return folderPath
        }
    }

    //------------------------------- Media Updater / clear cache --------------------------------//
    fun updateMediaStore(
        context: Context,
        filePath: String,
        name: String,
        completeListener: MediaScannerConnection.OnScanCompletedListener? = null,
        progressListener: MediaScannerConnection.OnScanCompletedListener? = null
    ) {
        Log.d(TAG, "File Path and name is == $filePath, $name")
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")

            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/SlowMoMaker"
            )
        }

        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        // Notify media scanner about the new file
        MediaScannerConnection.scanFile(
            context,
            arrayOf(File(filePath).absolutePath),
            arrayOf("video/mp4"),
            completeListener
        ).apply {
            progressListener?.let { }
        }
    }

    val completeListener =
        MediaScannerConnection.OnScanCompletedListener { path, uri -> // Handle completion of scanning here
            Log.d(TAG, "Scan completed for $path")
        }

    val progressListener =
        MediaScannerConnection.OnScanCompletedListener { path, uri -> // Handle progress of scanning here
            Log.d(TAG, "Scanning progress for $path")
        }

    fun clearCacheDirectory(cacheDir: File): Boolean {
        // Check if the cache directory exists
        if (!cacheDir.exists() || !cacheDir.isDirectory) {
            return false
        }
        // Get list of files and directories within cache directory
        val files = cacheDir.listFiles()
        // Iterate through each file and directory and delete them
        files?.forEach { file ->
            if (file.isDirectory) {
                // Recursively delete subdirectories
                clearCacheDirectory(file)
            } else {
                // Delete file
                file.delete()
            }
        }
        // Check if cache directory is empty after deletion
        return cacheDir.listFiles()?.isEmpty() ?: true
    }

    //------------------------------------- Frames View Utils ------------------------------------//
    @SuppressLint("SuspiciousIndentation")
    fun framesFromVideo(
        context: Context?,
        frames: HashSet<Bitmap>,
        videoUri: Uri, videoFrameView: VideoFrameView
    ) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(videoUri.toString())
                mediaPlayer.prepare()
                val videoDuration = mediaPlayer.duration
                duration = videoDuration
                var tempDuration = videoDuration

                tempDuration /= 1000
                mediaPlayer.release()

                /** Callback For Bar */

                withContext(Dispatchers.Main) {
                    frameCallback?.invoke(videoDuration)
                }

                if (frames.isEmpty()) {
                    val frameCount = 10 // the number of frames to display
                    val frameInterval =
                        videoDuration / frameCount // the time interval between each frame
                    val frameWidth = 120 // the desired width of the frame
                    val frameHeight = 120 // the desired height of the frame

                    val cache = mutableMapOf<Long, Bitmap>() // cache of previously retrieved frames

                    for (i in 0 until frameCount) {
                        val frameTime = i * frameInterval
                        val timeUs = (frameTime * 1000).toLong()

                        val frame = frameGetter(context, videoUri, timeUs, frameWidth, frameHeight)
                        frame?.let {
                            frames.add(it)
                            cache[timeUs] = it // store the retrieved frame in the cache
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    videoFrameView.displayFrames(frames)
                }
            } catch (e: Exception) {
                e.localizedMessage?.let { Log.d(TAG, it) }
            }
        }
    }


    private fun frameGetter(
        context: Context?,
        uri: Uri?,
        time: Long,
        width: Int,
        height: Int
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            bitmap = retriever.getFrameAtTime(time)
            bitmap = bitmap?.let { Bitmap.createScaledBitmap(it, width, height, false) }
        } catch (ex: RuntimeException) {
            ex.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (ex: RuntimeException) {
                ex.printStackTrace()
            }
        }
        return bitmap
    }

    //------------------------------------- Video View Utils -------------------------------------//
    fun speedCalculator(speedBarProgress: Int) {
        when (speedBarProgress) {
            in 20..25 -> {
                speedUnit = 0.45f
                speedForFFmpeg = 2F
            }

            in 25..30 -> {
                speedUnit = 0.50f
                speedForFFmpeg = 1.8F
            }

            in 30..35 -> {
                speedUnit = 0.60f
                speedForFFmpeg = 1.7F
            }

            in 35..40 -> {
                speedUnit = 0.65f
                speedForFFmpeg = 1.5F
            }

            in 40..45 -> {
                speedUnit = 0.75f
                speedForFFmpeg = 1.43F
            }

            in 45..50 -> {
                speedUnit = 1f
                speedForFFmpeg = 1F
            }

            in 50..55 -> {
                speedUnit = 1.5f
                speedForFFmpeg = 0.67F
            }

            in 55..60 -> {
                speedUnit = 2f
                speedForFFmpeg = 0.5F
            }

            in 60..65 -> {
                speedUnit = 2.5f
                speedForFFmpeg = 0.4F
            }

            in 65..70 -> {
                speedUnit = 3f
                speedForFFmpeg = 0.3F
            }

            in 70..75 -> {
                speedUnit = 3.5f
                speedForFFmpeg = 0.27F
            }

            in 75..80 -> {
                speedUnit = 4f
                speedForFFmpeg = 0.25F
            }
        }
    }
}
