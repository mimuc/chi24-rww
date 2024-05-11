package com.mimuc.rww

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.mimuc.rww.FirebaseConfig.Companion.initFirebasePaths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class LoginActivity : AppCompatActivity() {

    companion object {
        lateinit var mAuth: FirebaseAuth
        lateinit var id: String
        const val SHARED_PREFS = "sharedPrefs"
        const val USER_ID = "user_id"
        const val USER_ID_ON_START = "user_id_on_start"
    }

    override fun onStart() {
        val user = mAuth.currentUser

        // sets up everything and starts Main
        if (user != null) {
            val sharedPrefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
            id = sharedPrefs.getString(USER_ID_ON_START, null).toString()
            initFirebasePaths(id)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
            FirebaseConfig.debugUnlocksRef?.push()?.setValue(timeNow + ": Login onStart -> startActivity called")

            finish()
        }
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)
        mAuth = FirebaseAuth.getInstance()

        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener {

            id = findViewById<EditText>(R.id.editTextNumber).text.toString()

            if (id.length < 4) {
                Toast.makeText(this, "Please use your StudyID (Aa-Zz, 0-9)", Toast.LENGTH_SHORT).show()
            }
            else if ("^[A-Za-z0-9_-]+$".toRegex().matches(id)) {
                createUserAccount(id)

                //val sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
                //val userIdStartsWith = sharedPreferences.getString(USER_ID, "")?.startsWith("1") == true
                /**if (userIdStartsWith) {
                    if (checkUsageStatsPermission()) {
                        val logESDialog = DialogLogExperienceSampling()
                        logESDialog.show(supportFragmentManager, "log_ES")
                    } else {
                        val usageStatsDialog = DialogUsageStats()
                        usageStatsDialog.isCancelable = false
                        usageStatsDialog.show(supportFragmentManager, "usageStatsDialog")
                    }
                } else {**/
                    if (checkUsageStatsPermission()) {

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                        val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
                        FirebaseConfig.debugUnlocksRef?.push()?.setValue(timeNow + ": Login onCreate -> startActivity called")

                    } else {
                        val usageStatsDialog = DialogUsageStats()
                        usageStatsDialog.isCancelable = false
                        usageStatsDialog.show(supportFragmentManager, "usageStatsDialog")
                    }
                //}
            }
            else if(id == "") {
                Toast.makeText(this, "Enter StudyID", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Please do not use space or other special characters", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())
        FirebaseConfig.debugDestroyRef?.push()?.setValue(timeNow + " onDestroy Login")
    }


    private fun createUserAccount(id: String) {
        createDefaultChallenges()
        val email = "$id@realworldwind.com"
        val password = "userstudy"

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                initFirebasePaths(id)

                val sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString(USER_ID, id)
                editor.putString(USER_ID_ON_START, id)
                editor.apply()
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // adds default set of challenges once to shared prefs
    fun createDefaultChallenges() {
        val sharedPrefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
        if (!sharedPrefs.contains("jsonChallenge")) {
            val challengeList: ArrayList<Challenge> = ChallengeList().getMyList()

            val json = Gson().toJson(challengeList)
            val editor = sharedPrefs?.edit()
            editor?.putString("jsonChallenge", json)
            editor?.commit() //or apply()?
        }
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOpsManager: AppOpsManager?
        val mode: Int
        appOpsManager = getSystemService(Context.APP_OPS_SERVICE)!! as AppOpsManager
        mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
}