package com.mimuc.rww

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.mimuc.rww.R

object NotificationHelper {

    const val CHANNEL_ID_ONE = "channel_id_one"
    const val CHANNEL_NAME_ONE = "My service"
    const val CHANNEL_ID_TWO = "channel_id_two"
    const val CHANNEL_NAME_TWO = "Challenge"

    const val NOTIFICATION_ID = 7317


    fun createServiceNotificationChannel(context: Context) {
        //SDK_INT always >= 26
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID_ONE,
                CHANNEL_NAME_ONE,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.createNotificationChannel(notificationChannel)
        //}
    }

    fun createChallengeNotificationChannel(context: Context) {
        //SDK_INT always >= 26
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID_TWO,
                CHANNEL_NAME_TWO,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.createNotificationChannel(notificationChannel)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            FirebaseConfig.debugUnlocksRef?.push()?.setValue("inside NotificationHelper build: active notification size: "+manager.activeNotifications.size.toString())

        // }
    }

    fun sendChallengeNotification(context: Context, challengeName: String) {

        val replyIntent = Intent(context, NotificationReceiver::class.java)
        replyIntent.putExtra("challengeName", challengeName)

        createChallengeNotificationChannel(context)

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)


        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_TWO)
        val notification = notificationBuilder
            .setSmallIcon(R.drawable.challenge)
            .setContentTitle("New challenge!")
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentText(challengeName)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        FirebaseConfig.debugUnlocksRef?.push()?.setValue("inside NotificationHelper build: active notification size: "+notificationManager.activeNotifications.size.toString())

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        FirebaseConfig.debugUnlocksRef?.push()?.setValue("inside NotificationHelper notify: active notification size: "+notificationManager.activeNotifications.size.toString())

    }
}