package com.mimuc.rww

import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

const val MAX_SCREENTIME:Int = 720   //720s=12mins
const val MAX_NUMBER_OF_UNLOCKS:Int = 6    //6
const val TIME_RESET_FOR_UNLOCKS:Int = 1800   //1800s=30mins

class ScreenReceiver : BroadcastReceiver() {
    var minute = 0
    var hour = 0
    var day = 0
    var month = 0
    var year = 0

    val timestamps : MutableList<Long> = mutableListOf()
    
    override fun onReceive(context: Context, intent: Intent?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationIntent = Intent(context, ScreenNotificationReceiver::class.java)

        println("ScreenTimeService onReceive")

        // broadcast screen-on received
        if (intent?.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            println("SCREEN ON")
            try {
                createNotificationChannel(context)
                showNotification(context, "screentime", alarmManager, notificationIntent)
            } catch (e: Exception) {
                val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In ScreenReceiver 'intent?.getAction().equals(Intent.ACTION_SCREEN_ON) showNotification': " + e)
            }
        }

        // broadcast screen-off received
        if (intent?.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            println("SCREEN OFF")
            try {
                stopTimer(context, alarmManager, notificationIntent)
            } catch (e: Exception) {
                val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In ScreenReceiver 'intent?.getAction().equals(Intent.ACTION_SCREEN_OFF)': " + e)
            }
        }

        // broadcast unlock received
        if (intent?.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            println("UNLOCK USER PRESENT")
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            val nowUnix = System.currentTimeMillis() / 1000L
            timestamps.add(nowUnix)
            println(timestamps.toString())
            var index = 0
            var timestampRemoved = false

            try {
                for ((i, timestamp) in timestamps.asReversed().withIndex()) {
                    if (isTimeExceeded(timestamp, nowUnix, TIME_RESET_FOR_UNLOCKS)) {
                        index = timestamps.size - i - 1
                        timestampRemoved = true
                        break
                    } else {
                        timestampRemoved = false
                    }
                }
            } catch (e: Exception) {
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In ScreenReceiver 'intent?.getAction().equals(Intent.ACTION_USER_PRESENT) for-loop timestamps': " + e)
            }

            try {
                if (timestampRemoved) {
                    for (x in 0..index) {
                        timestamps.removeAt(0)
                    }
                }
            } catch (e: Exception) {
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In ScreenReceiver 'intent?.getAction().equals(Intent.ACTION_USER_PRESENT) if timestampRemoved': " + e)
            }

            FirebaseConfig.debugUnlocksRef?.push()?.setValue(timeNow + ": timestamps.size: "+timestamps.size.toString())
            if (timestamps.size == MAX_NUMBER_OF_UNLOCKS) {
                println("found potential smartphone overload by too many unlocks per time")
                timestamps.clear()
                try {
                    FirebaseConfig.debugUnlocksRef?.push()?.setValue(timeNow + ": Found potential smartphone overload by too many unlocks per time")
                    showNotification(
                        context,
                        "number of unlocks",
                        alarmManager,
                        notificationIntent
                    )
                } catch (e: Exception) {
                    FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In ScreenReceiver 'intent?.getAction().equals(Intent.ACTION_USER_PRESENT) showNotification': " + e)
                }
            }
        }
    }

    private fun showNotification(context:Context, reason: String, alarmManager:AlarmManager, notificationIntent:Intent) {
            val title = "Potential smarthone overload"
            val message = "caused by " + reason

            notificationIntent.putExtra(notificationID.toString(), 1)
            notificationIntent.putExtra(titleExtra, title)
            notificationIntent.putExtra(messageExtra, message)
            notificationIntent.putExtra("reason", reason)
            notificationIntent.putExtra(
                "toastMessage",
                "Potential smartphone overload " + message + " detected"
            )

            var time: Long = 0
            if (reason == "screentime") {
                time = setTimer(MAX_SCREENTIME)
                //showAlert(context, time, title, message)
            } else if (reason == "number of unlocks") {
                time = setCalender().timeInMillis
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationID,
                notificationIntent,
                FLAG_UPDATE_CURRENT
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, time, pendingIntent
            )
    }

    // sets calender with current time
    private fun setCalender(): Calendar {
        val timestamp = LocalDateTime.now().toString() //2017-08-02T11:25:44.973
        minute = timestamp.substring(14, 16).toInt()
        hour = timestamp.substring(11, 13).toInt()
        day = timestamp.substring(8, 10).toInt()
        month = timestamp.substring(5, 7).toInt()-1
        year = timestamp.substring(0, 4).toInt()

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute)
        return calendar
    }

    // sets timer for overload-notification
    private fun setTimer(screentime:Int): Long {
        val calendar = setCalender()
        calendar.add(GregorianCalendar.SECOND, screentime)
        return calendar.timeInMillis
    }

    // stops timer for overload-notification (after screen off)
    private fun stopTimer(context:Context, alarmManager: AlarmManager, notificationIntent:Intent): Long {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID,
            notificationIntent,
            FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)

        minute = 0
        hour = 0
        day = 0
        month = 0
        year = 0

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute)
        return calendar.timeInMillis
    }

    //return true if time difference > 30min: then timestamp will be dropped from list
    private fun isTimeExceeded(timestamp: Long, now:Long, duration:Int):Boolean {
        return timestamp + duration < now
    }

    private fun createNotificationChannel(context: Context?) {
        val name = "Notif Channel"
        val desc = "This is our notification channel"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc

        try {
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In ScreenReceiver createNotificationChannel() method: " + e)
        }
    }
}