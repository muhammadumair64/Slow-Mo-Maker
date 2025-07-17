package com.iobits.photo_to_video_slides_maker.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.iobits.photo_to_video_slides_maker.R
import com.iobits.photo_to_video_slides_maker.ui.activities.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

class NotificationUtil @Inject constructor() {

    fun sendNotification(
        progress:Int,
        messageTitle: String,
        messageBody: String,
        notificationSound: Uri?,
        notificationId: Int,
        context: Context,
        ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//            App.comingFromIntent=true
        }
//        val pendingIntent = PendingIntent.getActivity(
//            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
//        )

        val channelId = "notification_channel"
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(context, channelId).setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(messageBody).setAutoCancel(true)
                .setProgress(100,progress,false)



        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Custom Notification Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)

            val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

            // Set custom sound for the channel
            notificationSound.let {
                channel.setSound(it, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
        return notificationBuilder.build()
    }
}