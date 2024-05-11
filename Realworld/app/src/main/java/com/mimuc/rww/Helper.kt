package com.mimuc.rww

import android.app.AppOpsManager
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.content.SharedPreferences
import androidx.core.content.ContextCompat.getSystemService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Helper {

    val gson: Gson = Gson()


    // returns array list of challenges from shared prefs
    fun getCurrentChallengeListFromPref(gson: Gson, sharedPrefs: SharedPreferences?): ArrayList<Challenge?> {
        val savedList = sharedPrefs?.getString("jsonChallenge", "?")
        val myType = object : TypeToken<ArrayList<Challenge>>() {}.type
        val list = gson.fromJson<ArrayList<Challenge?>>(savedList, myType)
        return list
    }

    // returns list of challenges belonging to chosen categories
    fun getChosenChallenges(sharedPrefs: SharedPreferences?, relaxingChecked: Boolean?, mentalChecked: Boolean?, physicalChecked: Boolean?, socialChecked: Boolean?, organizingChecked: Boolean?, miscChecked: Boolean?, personalizedChecked: Boolean?, randomChecked: Boolean?): java.util.ArrayList<Challenge?> {
        val existingChallenges = getCurrentChallengeListFromPref(gson, sharedPrefs)
        var chosenChallenges: java.util.ArrayList<Challenge?> = java.util.ArrayList()
        val personalizedChallenges: java.util.ArrayList<Challenge?> = java.util.ArrayList()

        if (randomChecked == true) {
            return existingChallenges
        } else {
            if (personalizedChecked == true) {
                for (item in existingChallenges) {
                    if (item?.personalized == true) {
                        personalizedChallenges.add(item)
                    }
                }
                for (item in personalizedChallenges) {
                    if (item?.cat?.equals(Category.RELAXING) == true && relaxingChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.MENTAL) == true && mentalChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.PHYSICAL) == true && physicalChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.SOCIAL) == true && socialChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.ORGANIZING) == true && organizingChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.MISC) == true && miscChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if ((relaxingChecked == false)&&
                        (mentalChecked == false)&&
                        (physicalChecked == false)&&
                        (socialChecked == false)&&
                        (miscChecked == false)&&
                        (organizingChecked == false)) {
                        chosenChallenges = personalizedChallenges
                    }
                }
            } else {
                for (item in existingChallenges) {
                    if (item?.cat?.equals(Category.RELAXING) == true && relaxingChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.MENTAL) == true && mentalChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.PHYSICAL) == true && physicalChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.SOCIAL) == true && socialChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.ORGANIZING) == true && organizingChecked == true) {
                        chosenChallenges.add(item)
                    }
                    if (item?.cat?.equals(Category.MISC) == true && miscChecked == true) {
                        chosenChallenges.add(item)
                    }
                }
            }
            return chosenChallenges
        }
    }

    fun convertTime(lastTimeUsed: Long): String {
        val date = Date(lastTimeUsed * 1L)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.GERMAN)
        return format.format(date)
    }

}