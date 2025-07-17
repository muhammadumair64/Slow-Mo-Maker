package com.iobits.photo_to_video_slides_maker.videoCompressor.config

import com.iobits.photo_to_video_slides_maker.videoCompressor.VideoQuality

data class Configuration(
    var quality: VideoQuality = VideoQuality.MEDIUM,
    var isMinBitrateCheckEnabled: Boolean = true,
    var videoBitrateInMbps: Int? = null,
    var disableAudio: Boolean = false,
    val keepOriginalResolution: Boolean = false,
    var videoHeight: Double? = null,
    var videoWidth: Double? = null,
    var videoNames: List<String>
)

data class AppSpecificStorageConfiguration(
    var subFolderName: String? = null,
)

data class SharedStorageConfiguration(
    var saveAt: SaveLocation? = null,
    var subFolderName: String? = null,
)

enum class SaveLocation {
    pictures,
    downloads,
    dcim,
}