package com.mimuc.rww

import Category
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.mimuc.rww.FirebaseConfig.Companion.completedChallengesRef
import com.mimuc.rww.FirebaseConfig.Companion.logsRef
import com.mimuc.rww.FirebaseConfig.Companion.myRootRef
import com.mimuc.rww.LoginActivity.Companion.SHARED_PREFS
import com.mimuc.rww.LoginActivity.Companion.USER_ID
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {

    var challenges = ArrayList<Challenge?>()
    val recyclerViewAdapter = RecyclerViewAdapter(challenges)
    var currentChallengeActive: Boolean = false
    var challenge: Challenge? = null
    val screentimeReceiver = ScreenReceiver()


    override fun onCreate(savedInstanceState: Bundle?) {
        
        FirebaseConfig.debugUnlocksRef?.push()?.setValue("in Main onCreate")

        // turns off night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // sets night mode

        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate setContentView(): " + e)
        }

        /**
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val serviceIntent = Intent(this, MyService::class.java)
                this.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate startForegroundService(): " + e)
            }
        } else {**/

        //SDK_INT always >= 26
        try {
            val serviceIntent = Intent(this, MyService::class.java)
            this.startService(serviceIntent)
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()
                ?.setValue(timeNow + ": Exception In MainActivity onCreate startService(): " + e)
        }
        // }

        // handles notification replies
        val remoteInputBundle: Bundle? = RemoteInput.getResultsFromIntent(intent)
        if (remoteInputBundle != null) {
            val replyText: CharSequence? = remoteInputBundle.getCharSequence("key_challenge_reply")
            challenge = (intent.getSerializableExtra("notification_challenge") as Challenge?)

            val answerIntent = Intent("completed_fn")
            val answer: String = replyText.toString()
            answerIntent.putExtra("answer", answer)
            answerIntent.putExtra("is_from_nf", true)
            answerIntent.putExtra("notification_challenge", challenge)

            let { LocalBroadcastManager.getInstance(this).sendBroadcast(answerIntent) }
        }

        val receivedIntent = intent // gets the previously created intent
        val firstKeyName = receivedIntent.getStringExtra("firstKeyName")

        // handles notification cancels
        if (firstKeyName == "send_cancel_fn") {
            challenge = (receivedIntent.getSerializableExtra("randomChallengeIntent") as Challenge)

            val intentCancelInMain = Intent("cancel_challenge_fn")
            intentCancelInMain.putExtra("challengeIntentForService", challenge)
            intentCancelInMain.putExtra("validateStringForService", "validate_service")
            LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, IntentFilter("cancel_challenge_fn"))

            this.let { LocalBroadcastManager.getInstance(this).sendBroadcast(intentCancelInMain) }
        }

        // handles notification exchanges
        if (intent.getStringExtra("firstKeyName") == "send_exchange_fn") {
            challenge =
                (receivedIntent.getSerializableExtra("exchangedChallengeIntent") as Challenge)

            val intentExchangedInMain = Intent("switch")
            intentExchangedInMain.putExtra("challengeIntentForService", challenge)
            intentExchangedInMain.putExtra("validateStringForService", "validate_service")
            LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, IntentFilter("switch"))

            this.let {
                LocalBroadcastManager.getInstance(this).sendBroadcast(intentExchangedInMain)
            }
        }

        // sets filter for broadcast receiver
        try {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            filter.addAction(Intent.ACTION_USER_PRESENT)
            registerReceiver(screentimeReceiver, filter)
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()
                ?.setValue(timeNow + ": Exception In MainActivity onCreate filter.addAction: " + e)
        }

        val sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
        val userIdStartsWith = sharedPreferences.getString(USER_ID, "")?.startsWith("1") == true

        val firstStart = sharedPreferences.getBoolean("firstStart", true)
        if (firstStart) {
            try {
                val firstStartTime = Calendar.getInstance()
                val editor = sharedPreferences.edit()
                editor.putLong("firstStartTime", firstStartTime.timeInMillis)
                editor.putBoolean("firstStart", false)
                editor.apply()
                //Toast.makeText(this, "First start: ${simpleDateFormat.format(firstStartTime.timeInMillis)}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate firstStart: " + e)
            }
        }

        
        if (userIdStartsWith) {
            if (logsRef != null) {
                logsRef?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            challenges.clear()

                            for (postSnapshot in snapshot.children) {
                                val challenge = postSnapshot.getValue(Challenge::class.java)
                                challenges.add(challenge)
                            }

                            if (challenges.isNotEmpty()) {
                                challenges.reverse()
                            }
                            val challengerino = Challenge("Log Header", Category.MENTAL, false)
                            challenges.add(0, challengerino)

                            recyclerViewAdapter.notifyDataSetChanged()
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate if userIdStartsWith onDataChange: " + e)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
        } else {
            try {
                setFragment()
            } catch (e: Exception) {
                val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate !if userIdStartsWith setFragment(): " + e)
            }
            if (completedChallengesRef != null) {
                completedChallengesRef?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            challenges.clear()
                            for (postSnapshot in snapshot.children) {
                                val challenge = postSnapshot.getValue(Challenge::class.java)
                                challenges.add(challenge)
                            }
                            if (challenges.isNotEmpty()) {
                                challenges.reverse()
                            }
                            //TODO
                            val challengerino =
                                Challenge("Challenge Header", Category.MENTAL, false)
                            challenges.add(0, challengerino)

                            recyclerViewAdapter.notifyDataSetChanged()
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate if completedChallengesRef onDataChange: " + e)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }

        // floatingActionButton for adding own challenges
        val fabAdd = findViewById<FloatingActionButton>(R.id.floatingActionButtonAdd)
        fabAdd.setOnClickListener {
            val addDialog = DialogAddChallenge()
            try {
                addDialog.show(supportFragmentManager, "dialogAddChallenge")
            } catch (e: Exception) {
                val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate fabAdd.setOnClickListener: " + e)
            }
        }

        // floatingActionButton for snoozing up to 60mins
        val fabSnooze = findViewById<FloatingActionButton>(R.id.floatingActionButtonSnooze)
        fabSnooze.setOnClickListener{
            val snoozeDialog = DialogSnooze()
            try {
                snoozeDialog.show(supportFragmentManager, "dialogSnooze")
            } catch (e: Exception) {
                val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate fabSnooze.setOnClickListener: " + e)
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recyclerViewAdapter

        // registers LocalBroadcastManagers for handling own broadcasts
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter("completed"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter("completed_challenge_fn"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter("upload_challenge_from_notification"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("switch"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("send_reason"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("send_cancel_reason"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("intentKey"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("evaluate_challenge"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("cancel_challenge"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("dismiss_cancel_hide_dialog"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("send_hidden_challenge"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("log_from_tile"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("fab"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter("cat_chosen"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mNotificationReceiver, IntentFilter("challenge_received"))


        val placeholderFragment = PlaceHolderFragment()
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, placeholderFragment).commit()
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate if userIdStartsWith placeHolderFragment: " + e)
        }
    }


    override fun onStart() {
        super.onStart()
        createReminderChannel()
        scheduleReminder()
    }


    override fun onBackPressed() {
        moveTaskToBack(true)
    }


    override fun onDestroy() {
        super.onDestroy()
        try{
            unregisterReceiver(screentimeReceiver)}
        catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception unregisterReceiver not possible")
        }

        val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
        FirebaseConfig.debugDestroyRef?.push()?.setValue(timeNow + " onDestroy Main")
        finish()
    }

    // handles incoming intents
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val receivedIntent = intent // gets the previously created intent
        val firstKeyName = receivedIntent?.getStringExtra("firstKeyName")

        // handles notification replies
        val remoteInputBundle: Bundle? = RemoteInput.getResultsFromIntent(intent)
        if (remoteInputBundle != null) {
            val replyText: CharSequence? = remoteInputBundle.getCharSequence("key_challenge_reply")
            challenge = (intent?.getSerializableExtra("notification_challenge") as Challenge?)
            val newIntent = Intent("completed_fn")
            val answer: String = replyText.toString()
            newIntent.putExtra("answer", answer)
            newIntent.putExtra("is_from_nf", true)
            newIntent.putExtra("notification_challenge", challenge)

            try {
                let { LocalBroadcastManager.getInstance(this).sendBroadcast(newIntent) }
            } catch (e: Exception) {
                val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception in Main onNewIntent sendBroadcast")
            }
        }

        // handles notification cancels
        if (firstKeyName == "send_cancel_fn") {
            challenge = (receivedIntent.getSerializableExtra("randomChallengeIntent") as Challenge)

            val intentCancelInMain = Intent("cancel_challenge_fn")
            intentCancelInMain.putExtra("challengeIntentForService", challenge)
            intentCancelInMain.putExtra("validateStringForService", "validate_service")
            LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, IntentFilter("cancel_challenge_fn"))

            this.let { LocalBroadcastManager.getInstance(this).sendBroadcast(intentCancelInMain) }
        }

        // handles notification exchanges
        if (intent?.getStringExtra("firstKeyName") == "send_exchange_fn") {
            challenge =
                (receivedIntent?.getSerializableExtra("exchangedChallengeIntent") as Challenge)

            val intentExchangedInMain = Intent("switch")
            intentExchangedInMain.putExtra("challengeIntentForService", challenge)
            intentExchangedInMain.putExtra("validateStringForService", "validate_service")

            this.let {
                LocalBroadcastManager.getInstance(this).sendBroadcast(intentExchangedInMain)
            }
        }

        // handles reminder notifications
        if (intent?.getStringExtra("firstKeyName") == "alarmintent") {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(reminderID)
            manager.cancelAll()
        }
    }

    override fun onResume() {
        super.onResume()
        try{
            if (completedChallengesRef != null) {
                completedChallengesRef?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            challenges.clear()
                            for (postSnapshot in snapshot.children) {
                                val challenge = postSnapshot.getValue(Challenge::class.java)
                                challenges.add(challenge)
                            }
                            if (challenges.isNotEmpty()) {
                                challenges.reverse()
                            }
                            val challengerino =
                                Challenge("Challenge Header", Category.MENTAL, false)
                            challenges.add(0, challengerino)
                            recyclerViewAdapter.notifyDataSetChanged()
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onCreate if completedChallengesRef onDataChange: " + e)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
                }
        } catch(e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onResume recyclerview: " + e)
        }

        try {
            setFragment()
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity onResume setFragment: " + e)
        }
    }

    private fun setFragment() {
        myRootRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentChallengeFragment = CurrentChallengeFragment()
                if (snapshot.hasChild("open")) {
                    if (!supportFragmentManager.isDestroyed()) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, currentChallengeFragment).commit()
                        currentChallengeActive = true
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // handles incoming events received by intents
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // opens evaluation dialog
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val dialog = DialogEvaluateChallenge()
                try {
                    dialog.show(supportFragmentManager, "dialog_evaluate_challenge")
                } catch (e: Exception) {
                    val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                    FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mMessageReceiver isAtLeast(Lifecycle.State.RESUMED): " + e)
                }
            }
        }
    }

    // handles incoming events received by intents
    val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "switch" -> { // opens exchanging dialog
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val dialog = DialogSkipChallenge()
                        try {
                            dialog.show(supportFragmentManager, "costumDialog")
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->switch: " + e)
                        }
                    }
                }
                "send_reason" -> { // opens current challenge fragment
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val currentChallengeFragment = CurrentChallengeFragment()
                        try {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.frame_layout, currentChallengeFragment).commit()
                            currentChallengeActive = true
                            val fabIntent = Intent("fab")
                            LocalBroadcastManager.getInstance(context).sendBroadcast(fabIntent)
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->send_reason: " + e)
                        }
                    }
                }
                "send_cancel_reason" -> { // terminates challenge
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val placeholderFragment = PlaceHolderFragment()
                        try {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.frame_layout, placeholderFragment).commit()
                            currentChallengeActive = false
                            //fab.setImageResource(R.drawable.challenge_flag)
                            myRootRef?.child("open")?.removeValue()
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->send_cancel_reason: " + e)
                        }
                    }
                }
                "intentKey" -> {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val currentChallengeFragment = CurrentChallengeFragment()
                        try {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.frame_layout, currentChallengeFragment).commit()
                            currentChallengeActive = true
                            //fab.setImageResource(R.drawable.switch_challenge)
                            val myIntent = Intent("fab")
                            LocalBroadcastManager.getInstance(context).sendBroadcast(myIntent)
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->intentKey: " + e)
                        }
                    }
                }
                "evaluate_challenge" -> { // terminates challenge
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val placeholderFragment = PlaceHolderFragment()
                        try {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.frame_layout, placeholderFragment).commit()
                            currentChallengeActive = false
                            //fab.setImageResource(R.drawable.challenge_flag)
                            myRootRef?.child("open")?.removeValue()
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->evaluate_challenge: " + e)
                        }
                    }
                }
                "cancel_challenge" -> { // opens cancel dialog
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        if (challenge != null) {
                            val intentCancelChallenge = Intent("send_challenge_fn")
                            intentCancelChallenge.putExtra("challengeFromNotification", challenge)
                            context.let { it1 ->
                                LocalBroadcastManager.getInstance(it1)
                                    .sendBroadcast(intentCancelChallenge)
                            }
                        }
                        val cancelHideFragmentDialog = DialogCancelHideChallenge()
                        try {
                            cancelHideFragmentDialog.show(
                                supportFragmentManager,
                                "dialog_cancel_hide_challenge"
                            )
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->cancel_challenge: " + e)
                        }
                    }
                }
                "cancel_challenge_fn" -> { // opens cancel dialog from notification
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        if (challenge != null) {
                            val intentCancelChallenge = Intent("send_challenge_fn")
                            intentCancelChallenge.putExtra("challengeFromNotification", challenge)
                            context.let { it1 ->
                                LocalBroadcastManager.getInstance(it1)
                                    .sendBroadcast(intentCancelChallenge)
                            }
                        }
                        val cancelHideFragmentDialog = DialogCancelHideChallenge()
                        try {
                            cancelHideFragmentDialog.show(
                                supportFragmentManager,
                                "dialog_cancel_hide_challenge"
                            )
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->cancel_challenge_fn: " + e)
                        }
                    }
                }
                "dismiss_cancel_hide_dialog" -> { // opens cancel-hide dialog
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val cancelFragmentDialog = DialogCancelChallenge()
                        try {
                            cancelFragmentDialog.show(
                                supportFragmentManager,
                                "dialog_cancel_challenge"
                            )
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->dismiss_cancel_hide_dialog: " + e)
                        }
                    }
                }
                "fab" -> { // opens chosing-category dialog
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val choseCatDialog = DialogChoseCategory()
                        try {
                            choseCatDialog.show(supportFragmentManager, "dialogChoseCategory")
                            currentChallengeActive = true
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->fab: " + e)
                        }
                    }
                }
                "cat_chosen" -> { // passes on chosen category
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val currentChallengeFragment = CurrentChallengeFragment()
                        try {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.frame_layout, currentChallengeFragment).commit()
                            currentChallengeActive = true
                            //fab.setImageResource(R.drawable.switch_challenge)
                        } catch (e: Exception) {
                            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity mBroadcastReceiver->cat_chosen: " + e)
                        }
                    }
                }
            }
        }
    }

    // handles incoming events received by intents
    val mNotificationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "challenge_received" -> { // handles reactions on challenge
                    val firstKeyName = intent.getStringExtra("firstKeyName")

                    if (firstKeyName == "send_cancel_fn") {
                        challenge = (intent.getSerializableExtra("randomChallengeIntent") as Challenge)

                        val intentCancelInMain = Intent("cancel_challenge_fn")
                        intentCancelInMain.putExtra("challengeIntentForService", challenge)
                        intentCancelInMain.putExtra("validateStringForService", "validate_service")
                        LocalBroadcastManager.getInstance(context)
                            .registerReceiver(mBroadcastReceiver, IntentFilter("cancel_challenge_fn"))

                        this.let { LocalBroadcastManager.getInstance(context).sendBroadcast(intentCancelInMain) }
                    }
                    if (firstKeyName == "send_exchange_fn") {
                        challenge =
                            (intent.getSerializableExtra("exchangedChallengeIntent") as Challenge)
                        val intentExchangedInMain = Intent("switch")
                        intentExchangedInMain.putExtra("challengeIntentForService", challenge)
                        intentExchangedInMain.putExtra("validateStringForService", "validate_service")
                        LocalBroadcastManager.getInstance(context)
                            .registerReceiver(mBroadcastReceiver, IntentFilter("switch"))

                        this.let {
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intentExchangedInMain)
                        }
                }
    }}}}

    private fun checkUsageStatsPermission(): Boolean {
        var appOpsManager: AppOpsManager? = null
        var mode = 0
        appOpsManager = getSystemService(Context.APP_OPS_SERVICE)!! as AppOpsManager
        mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            applicationInfo.uid,
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // sends out random category
    fun giveChallenge() {
        try {
            if (checkUsageStatsPermission()) {
                val giveChallengeIntent = Intent("cat_chosen")
                giveChallengeIntent.putExtra("relaxingChecked", false)
                giveChallengeIntent.putExtra("mentalChecked", false)
                giveChallengeIntent.putExtra("physicalChecked", false)
                giveChallengeIntent.putExtra("socialChecked", false)
                giveChallengeIntent.putExtra("organizingChecked", false)
                giveChallengeIntent.putExtra("miscChecked", false)
                giveChallengeIntent.putExtra("randomChecked", true)
                this.let { it1 ->
                    LocalBroadcastManager.getInstance(it1).sendBroadcast(giveChallengeIntent)
                }
            } else {
                val usageStatsDialog = DialogUsageStats()
                usageStatsDialog.isCancelable = false
                usageStatsDialog.show(supportFragmentManager, "usageStatsDialog")
            }
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity giveChallenge() method: " + e)
        }
    }

    // opens exchange dialog
    fun switchChallenge() {
        val dialog = DialogSkipChallenge()
        try {
            dialog.show(supportFragmentManager, "costumDialog")
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In MainActivity switchChallenge() method: " + e)
        }
    }

    // snoozes BroadcastReceiver for given time by unregistering and re-registering
    fun snooze(minutes: Long) {
        val timeNow = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
        FirebaseConfig.snoozeRef?.push()?.setValue(timeNow + ": snoozed for " + minutes.toString() + "minutes")

        try {
            val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val notificationIntent = Intent(this, ScreenNotificationReceiver::class.java)
            stopTimer(this, alarmManager, notificationIntent)

            unregisterReceiver(screentimeReceiver)
        } catch (e: Exception) {
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In Main snooze() method 1: " + e)
        }

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)

        try {
            Timer("SnoozeTimer", false).schedule(minutes * 60 * 1000) {
                registerReceiver(screentimeReceiver, filter)
            }
        } catch (e: Exception) {
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In Main snooze() method 2: " + e)
        }
    }

    private fun stopTimer(context:Context, alarmManager: AlarmManager, notificationIntent:Intent) {
        try {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationID,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In Main stopTimer() method: " + e)
        }
    }

    //schedules daily reminder to check app status
    private fun scheduleReminder() {
        val intent = Intent(applicationContext, DailyReceiver::class.java)
        val title = "Click me!"
        val message = "Please make sure, that your challenges are displayed. Otherwise restart the app!"
        intent.putExtra(reminderTitleExtra, title)
        intent.putExtra(reminderMessageExtra, message)

        val reminderPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            reminderID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = getReminderTime()

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                reminderPendingIntent
            )

        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In Main scheduleReminder() method: " + e)
        }
    }

    private fun getReminderTime(): Long {
        val calendar: Calendar = Calendar.getInstance()

        try {
            val cur = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, 8)
            calendar.set(Calendar.MINUTE, 5)
            if (calendar.timeInMillis <= cur) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            } else {
                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    LocalDateTime.now().toString().substring(8, 10).toInt()
                )
            }
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In Main getReminderTime() method: " + e)
        }
        return calendar.timeInMillis
    }

    private fun createReminderChannel() {
        try {
            val name = "Reminder Channel"
            val desc = "A Desc of the Reminder Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(reminderChannelID, name, importance)
            channel.description = desc
            val reminderManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            reminderManager.createNotificationChannel(channel)
        } catch (e: Exception) {
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugRef?.push()?.setValue(timeNow + ": Exception In Main createReminderChannel() method: " + e)
        }
    }
}