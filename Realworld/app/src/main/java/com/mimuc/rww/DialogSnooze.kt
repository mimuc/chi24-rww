package com.mimuc.rww
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.mimuc.rww.databinding.DialogSnoozeBinding
import java.text.SimpleDateFormat
import java.util.*

class DialogSnooze: DialogFragment() {
    private var binding: DialogSnoozeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        binding = DialogSnoozeBinding.inflate(inflater, container, false)
        binding?.apply {

            snoozeButton.setOnClickListener {
                val snoozeTime: String = binding?.snoozeValue?.text.toString()
                val snoozeTimeLong: Long
                if(snoozeTime == "") {
                    snoozeTimeLong = 0
                } else {
                    snoozeTimeLong = snoozeTime.toLong()
                }

                if(snoozeTimeLong > 60) {
                    Toast.makeText(context, "You can only snooze up to 60 minutes", Toast.LENGTH_SHORT).show()
                }
                else if(snoozeTimeLong < 0) {
                    Toast.makeText(context, "Please enter a positive number", Toast.LENGTH_SHORT).show()
                }
                else if (1 <= snoozeTimeLong && snoozeTimeLong <= 60){
                    Toast.makeText(context, "Snoozing " + snoozeTime + " minutes", Toast.LENGTH_SHORT).show()
                    val timeNow = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.GERMAN).format(Date())

                    try {
                        (activity as MainActivity?)!!.snooze(snoozeTimeLong)
                    }

                    catch(e: Exception) {
                        FirebaseConfig.debugRef?.push()?.setValue("$timeNow: Exception in Snooze:  $snoozeTime minutes not possible")
                    }
                    FirebaseConfig.debugUnlocksRef?.push()?.setValue("$timeNow: starting $snoozeTime minute snooze")

                    dismiss()
                }
                else {
                    dismiss()
                }
            }
    }
        return binding?.root
    }
}