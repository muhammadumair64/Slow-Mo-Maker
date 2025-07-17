package com.iobits.photo_to_video_slides_maker.managers

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

object AnalyticsManager {
    private val firebaseAnalytics = Firebase.analytics
    fun logEvent(eventName: String, exception: String?) {
        firebaseAnalytics.logEvent(eventName) {
            exception?.let {
                param("$eventName: Exception", exception)
            }
        }
    }
}