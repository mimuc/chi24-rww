package com.mimuc.rww

import Category
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.mimuc.rww.databinding.DialogChoseCategoryBinding
import kotlin.collections.ArrayList

class DialogChoseCategory: DialogFragment() {

    private val helper = Helper()
    val gson: Gson = Gson()

        private var binding: DialogChoseCategoryBinding? = null

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?

        ): View? {
            binding = DialogChoseCategoryBinding.inflate(inflater, container, false)
            binding?.apply {

                val sharedPrefs = activity?.getSharedPreferences(LoginActivity.SHARED_PREFS, Context.MODE_PRIVATE)

                choseCategoriesButton.setOnClickListener {

                    val relaxingChecked = checkRelaxing.isChecked
                    val mentalChecked = checkMental.isChecked
                    val physicalChecked = checkPhysical.isChecked
                    val socialChecked = checkSocial.isChecked
                    val organizingChecked = checkOrganizing.isChecked
                    val miscChecked = checkMisc.isChecked

                    val personalizedChecked = checkPersonalized.isChecked

                    val catList: ArrayList<Category> = ArrayList()
                    if(relaxingChecked) {catList.add(Category.RELAXING)}
                    if(mentalChecked) {catList.add(Category.MENTAL)}
                    if(physicalChecked) {catList.add(Category.PHYSICAL)}
                    if(socialChecked) {catList.add(Category.SOCIAL)}
                    if(organizingChecked) {catList.add(Category.ORGANIZING)}
                    if(miscChecked) {catList.add(Category.MISC)}
                    if(personalizedChecked) {catList.add(Category.PERSONALIZED)}

                    val list: ArrayList<Challenge?> = helper.getChosenChallenges(sharedPrefs, relaxingChecked, mentalChecked, physicalChecked, socialChecked, organizingChecked, miscChecked, personalizedChecked, false)

                    if (list.isNotEmpty()) {
                        val intent = Intent("cat_chosen")

                        intent.putExtra("relaxingChecked", relaxingChecked)
                        intent.putExtra("mentalChecked", mentalChecked)
                        intent.putExtra("physicalChecked", physicalChecked)
                        intent.putExtra("socialChecked", socialChecked)
                        intent.putExtra("organizingChecked", organizingChecked)
                        intent.putExtra("miscChecked", miscChecked)
                        intent.putExtra("personalizedChecked", personalizedChecked)
                        intent.putExtra("randomChecked", false)

                        context?.let { it1 ->
                            LocalBroadcastManager.getInstance(it1).sendBroadcast(intent)
                        }

                        FirebaseConfig.chosenCategoriesRef?.push()?.setValue(catList)

                        dismiss()

                    } else {
                        Toast.makeText(context, "Your chosen categories are empty", Toast.LENGTH_SHORT).show()
                    }
                }
                randomCategoriesButton.setOnClickListener{
                    val list: ArrayList<Challenge?> = getRandomChallenge(sharedPrefs)

                    if (list.isNotEmpty()) {
                        val intent = Intent("cat_chosen")

                        intent.putExtra("relaxingChecked", false)
                        intent.putExtra("mentalChecked", false)
                        intent.putExtra("physicalChecked", false)
                        intent.putExtra("socialChecked", false)
                        intent.putExtra("organizingChecked", false)
                        intent.putExtra("miscChecked", false)
                        intent.putExtra("randomChecked", true)

                        val catList: ArrayList<Category> = ArrayList()
                        catList.add(Category.RANDOM)


                        context?.let { it1 ->
                            LocalBroadcastManager.getInstance(it1).sendBroadcast(intent)
                        }

                        FirebaseConfig.chosenCategoriesRef?.push()?.setValue(catList)

                        dismiss()

                    } else {
                        Toast.makeText(context, "There are no challenges for your profile. Please add your owns.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return binding?.root
        }

    private fun getRandomChallenge(sharedPrefs: SharedPreferences?): ArrayList<Challenge?> {
        return helper.getCurrentChallengeListFromPref(gson, sharedPrefs)
    }
}
