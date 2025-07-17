package com.iobits.photo_to_video_slides_maker.utils

object EditingOptionsValidator {
    var editorOptions = arrayListOf<String>()
    var isUsingOnlyTrimmer = false
    var isUsingOnlySpeed = false
    var isUsingOnlyReverse = false
    var commandMap: MutableMap<String, String> = mutableMapOf(
        Constants.slowMo to "",
        Constants.trim to "",
        Constants.music to "",
        Constants.reverse to "",
        Constants.textOnVideo to ""
    )
}