package com.mimuc.rww

import Category
import android.app.*
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.Intent.*
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mimuc.rww.FirebaseConfig.Companion.cancelledChallengesRef
import com.mimuc.rww.FirebaseConfig.Companion.completedChallengesRef
import com.mimuc.rww.FirebaseConfig.Companion.deletedChallengesRef
import com.mimuc.rww.FirebaseConfig.Companion.openChallengesRef
import com.mimuc.rww.FirebaseConfig.Companion.swappedChallengesRef
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MyService : Service() {

    private val helper = Helper()
    val gson: Gson = Gson()
    var openChallenges = ArrayList<Challenge?>()
    var challenges = mutableListOf<String?>()
    private lateinit var challengeName: String
    private var challenge: Challenge? = null
    var hidden = false

    private lateinit var challengeCategory: Category
    private var challengePersonalized: Boolean = false
    private var currentDate: String = ""
    private var currentTime: String = ""
    private var answer = ""
    private var reason = ""
    private var lastUsedApp = ""

    var start: Long = 0
    var stop: Long = 0
    var result: Long = -1


    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    companion object {
        const val CHANNEL_ID_ONE = "channel_id_one"
    }


    override fun onCreate() {
        super.onCreate()
        try {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugUnlocksRef?.push()?.setValue("$timeNow:  Service onCreate")
            NotificationHelper.createServiceNotificationChannel(this)
            createServiceNotification()
        }
        catch(e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception In MyService onCreate createServiceNotification(): $e")
        }
        // registers LocalBroadcastManagers for customized Broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, IntentFilter("intentKey"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, IntentFilter("cat_chosen"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mCompletedReceiver, IntentFilter("completed"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mCompletedReceiver, IntentFilter("completed_fn"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mCompletedReceiver, IntentFilter("upload_challenge_from_notification"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mCompletedReceiver, IntentFilter("send_reason"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mCompletedReceiver, IntentFilter("send_cancel_reason"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mCompletedReceiver, IntentFilter("send_hidden_challenge"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mCompletedReceiver, IntentFilter("send_challenge_fn"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mCompletedReceiver, IntentFilter("evaluate_challenge"))
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null){
            val sharedPrefs =  this.getSharedPreferences(LoginActivity.SHARED_PREFS, MODE_PRIVATE)
        try {
            NotificationHelper.createServiceNotificationChannel(this)
            createServiceNotification()
        }
        catch(i: IllegalStateException) {
        val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception In MyService onStartCommand: $i")
        }
        catch(e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception In MyService onStartCommand: $e")
        }

        if(!helper.getCurrentChallengeListFromPref(gson, sharedPrefs).equals("")) {
            openChallenges = helper.getCurrentChallengeListFromPref(gson, sharedPrefs)
        }
        val list = openChallenges
        if(list.isNotEmpty()) {
            for (item in list) {
                if (item != null) {
                    challenges.add(item.title)
                }
            }
        }

        openChallengesRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(openChallenges.isNotEmpty()) {
                    openChallenges.clear()
                }
                try {
                    for (postSnapshot in snapshot.children) {
                        val challenge = postSnapshot.getValue(Challenge::class.java)
                        openChallenges.add(challenge)
                    }
                }
                catch(e: Exception) {
                    val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                    FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception In MyService 'openChallengesRef?.addValueEventListener': $e")
                }
                if (openChallenges.isNotEmpty()){
                    try {
                        val lastPos = openChallenges.size - 1
                        if (challenge == null) {
                            challenge = Challenge(
                                //TODO
                                title = openChallenges[lastPos]?.title,
                                cat = openChallenges[lastPos]?.cat,
                                personalized = false,
                                time = openChallenges[lastPos]?.time,
                                date = openChallenges[lastPos]?.date,
                                notification = openChallenges[lastPos]?.notification,
                                viaNotification = openChallenges[lastPos]?.viaNotification,
                                "",
                                "",
                                "",
                                "",
                                "",
                                ""
                            )
                        }
                    } catch (e: Exception) {
                        val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                        FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception In MyService 'if (openChallenges.isNotEmpty())': $e")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        } else {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugDestroyRef?.push()?.setValue("$timeNow:  Service intent null!")
        }
        return START_STICKY // only relevant when phone runs out of memory and kills the service before it finishes executing. tells the OS to recreate the service after it has enough memory and call onStartCommand() again with a null intent
    }


    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Please restart the app", Toast.LENGTH_LONG).show()
        val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
        FirebaseConfig.debugDestroyRef?.push()?.setValue("$timeNow: onDestroy Service")
    }

    // returns array list of challenges from shared prefs
    /**private fun getCurrentChallengeListFromPref(
        gson: Gson,
        sharedPrefs: SharedPreferences?
    ): ArrayList<Challenge?> {
        val savedList = sharedPrefs?.getString("jsonChallenge", "?")
        val myType = object : TypeToken<ArrayList<Challenge?>>() {}.type
        return gson.fromJson(savedList, myType)
    }**/

    // sticky flag notification for foreground service
    private fun createServiceNotification() {
        try {
            val notificationIntent = Intent(this, MainActivity::class.java)
            notificationIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, FLAG_IMMUTABLE)
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_ONE)
            val notification = notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.challenge_flag)
                .setContentTitle("Real-World Wind is running in background.")
                .setContentText("Press here to log a new smartphone overload.")
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build()
            startForeground(2, notification)
        }
        catch(e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception In MyService createServiceNotification() method: $e")
        }
    }

    //uploads challenge to firebase as completed
    private fun uploadChallenge(){
        try {
            if (getLastUsedApp(challenge)) {
                setEndTime(challenge)
                challenge?.reason = null
                completedChallengesRef?.push()?.setValue(challenge)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            manager.cancel(notificationID)
            manager.cancelAll()
            FirebaseConfig.debugUnlocksRef?.push()?.setValue(timeNow + ": Active notification size in upload nach cancel: "+manager.activeNotifications.size.toString())
            challenge = null
        }
        catch(e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception In MyService uploadChallenge() method: $e")
        }
    }

    //uploads challenge to firebase as cancelled
    private fun uploadCancelledChallenge(){
        if (getLastUsedApp(challenge)){
            challenge?.agreeAwareness = null
            challenge?.agreeEnjoyed = null
            challenge?.agreeBored = null
            challenge?.agreeHappy = null
            challenge?.agreeAnnoyed = null
            challenge?.agreeWellbeing = null
            challenge?.agreeBalance = null
            challenge?.whichContext = null
            challenge?.answer = null
            stop = System.currentTimeMillis()
            result = stop - start
            val timeSinceUnlock: String = String.format(
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(result),
                TimeUnit.MILLISECONDS.toSeconds(result) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(result))
            )
            challenge?.timeSinceUnlock = timeSinceUnlock
            setEndTime(challenge)

            if(hidden) {
                deletedChallengesRef?.push()?.setValue(challenge)

            } else {
                cancelledChallengesRef?.push()?.setValue(challenge)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationID)
            manager.cancelAll()
            challenge = null
        }
    }

    //uploads challenge to firebase as exchanged
    private fun uploadSwappedChallenge(){
        try {
            challenge?.agreeAwareness = null
            challenge?.agreeEnjoyed = null
            challenge?.agreeBored = null
            challenge?.agreeHappy = null
            challenge?.agreeAnnoyed = null
            challenge?.agreeWellbeing = null
            challenge?.agreeBalance = null
            challenge?.whichContext = null
            challenge?.answer = null
            stop = System.currentTimeMillis()
            result = stop - start
            val timeSinceUnlock: String = String.format(
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(result),
                TimeUnit.MILLISECONDS.toSeconds(result) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(result))
            )
            challenge?.timeSinceUnlock = timeSinceUnlock
            setEndTime(challenge)

            swappedChallengesRef?.push()?.setValue(challenge)
        }
        catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception In MyService uploadSwappedChallenge() method: $e")
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationID)
        manager.cancelAll()
        challenge = null
    }

    // handles incoming events received by intents
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sharedPrefs: SharedPreferences = context.getSharedPreferences(LoginActivity.SHARED_PREFS, Context.MODE_PRIVATE)
            when (intent.action){
                "cat_chosen" -> { // opens a challenge after chosing a category
                    val relaxingChecked = intent.getExtras()?.getBoolean("relaxingChecked")
                    val mentalChecked = intent.getExtras()?.getBoolean("mentalChecked")
                    val physicalChecked = intent.getExtras()?.getBoolean("physicalChecked")
                    val socialChecked = intent.getExtras()?.getBoolean("socialChecked")
                    val organizingChecked = intent.getExtras()?.getBoolean("organizingChecked")
                    val miscChecked = intent.getExtras()?.getBoolean("miscChecked")
                    val personalizedChecked = intent.getExtras()?.getBoolean("personalizedChecked")
                    val randomChecked = intent.getExtras()?.getBoolean("randomChecked")
                    openChallenges = helper.getChosenChallenges(sharedPrefs, relaxingChecked, mentalChecked, physicalChecked, socialChecked, organizingChecked, miscChecked, personalizedChecked, randomChecked)
                    challenges.clear()
                    if(openChallenges != null) {
                        for (item in openChallenges) {
                            if (item != null) {
                                challenges.add(item.title)
                            }
                        }
                    }

                    val simpleDateFormatDate = SimpleDateFormat("dd/MM/yyyy", Locale.GERMAN)
                    val simpleDateFormatTime = SimpleDateFormat("HH:mm:ss", Locale.GERMAN)
                    currentDate = simpleDateFormatDate.format(Date())
                    currentTime = simpleDateFormatTime.format(Date())

                    if(openChallenges != null) {
                        val randomChallenge = openChallenges.random()
                        challengeName = randomChallenge?.title.toString()
                        challengeCategory = randomChallenge?.cat!!
                        val viaNotification = randomChallenge?.viaNotification
                        val notification = randomChallenge?.notification

                        challenge = Challenge(
                            title = challengeName,
                            cat = challengeCategory,
                            personalized = false,
                            time = currentTime,
                            date = currentDate,
                            notification = notification,
                            viaNotification = viaNotification,
                            answer = answer,
                            reason = reason,
                            lastUsedApps = lastUsedApp,
                        )

                        openChallengesRef?.push()?.setValue(challenge)
                    }
                }
                "intentKey" -> {
                    if(checkUsageStatsPermission()){
                        val simpleDateFormatDate = SimpleDateFormat("dd/MM/yyyy", Locale.GERMAN)
                        val simpleDateFormatTime = SimpleDateFormat("HH:mm:ss", Locale.GERMAN)
                        currentDate = simpleDateFormatDate.format(Date())
                        currentTime = simpleDateFormatTime.format(Date())
                        challengeName = challenges.random().toString()
                        val challengeNotification = null
                        val challengeViaNotification = false
                        challengeCategory = Category.MENTAL
                        challengePersonalized = false
                        challenge = Challenge(challengeName, challengeCategory, challengePersonalized, currentTime, currentDate, challengeNotification, challengeViaNotification, answer, reason, lastUsedApp)
                        openChallengesRef?.push()?.setValue(challenge)
                    }
                    else{
                        try {
                            startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(
                                    FLAG_ACTIVITY_NEW_TASK
                                )
                            )
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugUnlocksRef?.push()?.setValue(timeNow + ": startActivity in MyService intentKey")
                        }
                        catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MyService mMessageReceiver->intentKey 'else { startActiviy() }': " + e)
                        }
                    }
                }
            }
        }
    }

    // handles incoming events received by intents
    private val mCompletedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sharedPrefs: SharedPreferences = context.getSharedPreferences(LoginActivity.SHARED_PREFS, Context.MODE_PRIVATE)
            when (intent.action){
                "completed" -> { // adds given reply and times to challenge object
                    challenge?.answer = intent.getStringExtra("answer")

                    stop = System.currentTimeMillis()
                    result = stop - start
                    val timeSinceUnlock: String = String.format(
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(result),
                        TimeUnit.MILLISECONDS.toSeconds(result) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(result))
                    )
                    challenge?.timeSinceUnlock = timeSinceUnlock
                }
                "completed_fn" -> { // adds given reply and times to challenge object
                    val isFromNotification: Boolean? = intent.getExtras()?.getBoolean("is_from_nf")

                    challenge = null
                    if (isFromNotification == true) {
                        val notificationChallenge = intent.getSerializableExtra("notification_challenge") as Challenge
                        challenge = notificationChallenge
                    }
                    challenge?.answer = intent.getStringExtra("answer")

                    stop = System.currentTimeMillis()
                    result = stop - start
                    val timeSinceUnlock: String = String.format(
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(result),
                        TimeUnit.MILLISECONDS.toSeconds(result) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(result))
                    )
                    challenge?.timeSinceUnlock = timeSinceUnlock

                    val simpleDateFormatDate = SimpleDateFormat("dd/MM/yyyy", Locale.GERMAN)
                    val simpleDateFormatTime = SimpleDateFormat("HH:mm:ss", Locale.GERMAN)
                    currentDate = simpleDateFormatDate.format(Date())
                    currentTime = simpleDateFormatTime.format(Date())
                    challenge?.date = currentDate
                    challenge?.time = currentTime

                    val intentCompleted = Intent("completed_challenge_fn")
                    context.let { it1 -> LocalBroadcastManager.getInstance(it1).sendBroadcast(intentCompleted) }
                }
                "upload_challenge_from_notification" -> { // uploads challenge
                    challenge?.answer = intent.getStringExtra("answer")

                    stop = System.currentTimeMillis()
                    result = stop - start
                    val timeSinceUnlock: String = String.format(
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(result),
                        TimeUnit.MILLISECONDS.toSeconds(result) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(result))
                    )
                    challenge?.timeSinceUnlock = timeSinceUnlock
                    uploadChallenge()
                }
                "evaluate_challenge" -> { // adds evaluation to challenge object and uploads
                    challenge?.agreeAwareness = intent.getStringExtra("agreeAwareness")
                    challenge?.agreeEnjoyed = intent.getStringExtra("agreeEnjoyed")
                    challenge?.agreeBored = intent.getStringExtra("agreeBored")
                    challenge?.agreeHappy = intent.getStringExtra("agreeHappy")
                    challenge?.agreeAnnoyed = intent.getStringExtra("agreeAnnoyed")
                    challenge?.agreeWellbeing = intent.getStringExtra("agreeWellbeing")
                    challenge?.agreeBalance = intent.getStringExtra("agreeBalance")

                    challenge?.whichContext = intent.getStringExtra("whichContext")
                    uploadChallenge()
                }
                "send_reason" -> { // adds exchange-reason to challenge object and uploads
                    challenge?.reason = intent.getStringExtra("reason")
                    uploadSwappedChallenge()
                }
                "send_challenge_fn" -> { // gets initialized challenge from notification
                    if(!challengeInitialized()) {
                        challenge = intent.getSerializableExtra("challengeFromNotification") as Challenge
                    }
                }
                "send_hidden_challenge"-> { // adds hidden-flag to challenge object
                    val gotHidden = intent.getExtras()?.getBoolean("got_hidden")
                    val unlovedChallenge = challenge
                    val editor = sharedPrefs.edit()
                    if (gotHidden == true) {
                        alwaysHide(gson, sharedPrefs, editor, challenge)
                        hidden = true
                    } else {
                        hidden = false
                    }
                    challenge = unlovedChallenge
                }
                "send_cancel_reason" -> { // adds cancel-reason to challenge object and uploads
                    challenge?.reason = intent.getStringExtra("cancel_reason")
                    challenge?.feltOverload = intent.getExtras()?.getBoolean("check_feel_overload")
                    uploadCancelledChallenge()
                    challenge = null
                }
            }
        }
    }

    private fun challengeInitialized(): Boolean {
        return challenge != null
    }

    private fun getLastUsedApp(challenge: Challenge?): Boolean {
        val success: Boolean
        if (checkUsageStatsPermission()) {
                val usageStatsManager: UsageStatsManager =
                    getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val cal: Calendar = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_MONTH, -1)
                val queryUsageStats: List<UsageStats> = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    cal.timeInMillis,
                    System.currentTimeMillis()
                )
                val statsData = ArrayList<String>()

                // only those which are more than 1sec in foreground
                try {
                    for (i in 0..queryUsageStats.size - 1) {
                        if (
                            helper.convertTime(queryUsageStats[i].lastTimeUsed) != "01/01/1970 01:00:00" &&
                            !(queryUsageStats[i].packageName.contains("com.google")) &&
                            !(queryUsageStats[i].packageName.contains("com.android")) &&
                            !(queryUsageStats[i].packageName.contains("com.oneplus")) &&
                            !(queryUsageStats[i].packageName.contains("net.oneplus"))
                        ) {
                            statsData.add(
                                "Package Name: " + queryUsageStats[i].packageName + " --- " +
                                        "Last Time Used: " + helper.convertTime(queryUsageStats[i].lastTimeUsed) + "<br>"
                            )
                        }
                    }
                } catch (e: Exception) {
                    val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                    FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MyService getLastUsedApps() method for-loop: " + e)
                }
                challenge?.lastUsedApps = statsData.toString()
                success = true

            } else {
                try {
                    startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(
                            FLAG_ACTIVITY_NEW_TASK
                        )
                    )
                } catch (e: Exception) {
                    val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                    FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MyService getLastUsedApps 'else startActivity()': " + e)
                }
                success = false
            }
        return success
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOpsManager: AppOpsManager?
        var mode: Int = 0
        appOpsManager = getSystemService(Context.APP_OPS_SERVICE)!! as AppOpsManager
        mode = appOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS, applicationInfo.uid, packageName)
        return mode == MODE_ALLOWED
    }

    // removes challenge from shared prefs
    private fun removeChallengeFromPrefs(gson:Gson, sharedPrefs:SharedPreferences?, editor:SharedPreferences.Editor?, unlovedChallenge: Challenge?): ArrayList<Challenge?> {
        val list = helper.getCurrentChallengeListFromPref(gson, sharedPrefs)
        var key = -1
        for (item in list) {
            key = key + 1
            if (item?.title == unlovedChallenge?.title) {
                break
            }
        }
        list.removeAt(key)

        val json:String = gson.toJson(list)
        editor?.putString("jsonChallenge", json)
        editor?.commit()
        return list
    }

    // deletes challenge
    private fun alwaysHide(gson:Gson, sharedPrefs:SharedPreferences?, editor:SharedPreferences.Editor?, unlovedChallenge:Challenge?) {
        removeChallengeFromPrefs(gson, sharedPrefs, editor, unlovedChallenge)
    }

    private fun setEndTime(finishedChallenge: Challenge?) {
        val simpleDateFormatDate = SimpleDateFormat("dd/MM/yyyy", Locale.GERMAN)
        val simpleDateFormatTime = SimpleDateFormat("HH:mm:ss", Locale.GERMAN)
        currentDate = simpleDateFormatDate.format(Date())
        currentTime = simpleDateFormatTime.format(Date())
        finishedChallenge?.date = currentDate
        finishedChallenge?.endTime = currentTime
    }

}






