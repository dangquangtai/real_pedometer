package com.taivku.pedometer.fragment

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope


import com.taivku.pedometer.R
import com.taivku.pedometer.database.PersonalDatabase
import com.taivku.pedometer.database.PersonalDatabaseDAO
import com.taivku.pedometer.database.PersonalInfo
import com.taivku.pedometer.databinding.FragmentUserInfoBinding


import kotlinx.android.synthetic.main.fragment_user_info.*
import kotlinx.android.synthetic.main.fragment_user_info.view.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class UserInfoFragment : Fragment() {
    lateinit var binding: FragmentUserInfoBinding
    lateinit var database: PersonalDatabaseDAO
    var myPersonalInfo: PersonalInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_info, container, false)
        val application = requireNotNull(this.activity).application
        binding.lifecycleOwner = this
        database = PersonalDatabase.getInstance(application).personalDatabaseDAO
        val personalInfo = database.getListPersonalInfo()
        personalInfo.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it.isNotEmpty()) {
                    val thisPersonalInfo = it[0]
                    myPersonalInfo = it[0]
                    binding.inputAge.setText("" + thisPersonalInfo.age)
                    if (thisPersonalInfo.sex == "male") {
                        binding.radioGroup.maleChecked.isChecked = true
                    } else {
                        binding.radioGroup.femaleChecked.isChecked = true
                    }
                    binding.inputWeight.setText("" + thisPersonalInfo.weight)
                    binding.inputStepLength.setText("" + thisPersonalInfo.stepLength)
                    binding.inputHeight.setText("" + thisPersonalInfo.height)
                } else {
                    myPersonalInfo = PersonalInfo()
                    viewLifecycleOwner.lifecycleScope.launch {
                        database.insert(myPersonalInfo!!)
                    }
                    Toast.makeText(context, "null data", Toast.LENGTH_SHORT).show()
                }
            }
        })

        binding.btnConfirm.setOnClickListener {
            if (myPersonalInfo != null) {
                if (radioGroup.maleChecked.isChecked == true) {
                    myPersonalInfo!!.sex = "male"
                } else {
                    myPersonalInfo!!.sex = "female"
                }

                var weight = binding.inputWeight.text.toString()
                var stepLength = binding.inputStepLength.text.toString()
                var age = binding.inputAge.text.toString()
                var height = binding.inputHeight.text.toString()
                if (TextUtils.isEmpty(age) || TextUtils.isEmpty(weight) || TextUtils.isEmpty(stepLength)|| TextUtils.isEmpty(height)) {

                    Toast.makeText(context, "required not null !", Toast.LENGTH_SHORT).show()

                } else {
                    myPersonalInfo!!.weight = weight.toInt()
                    myPersonalInfo!!.stepLength = stepLength.toInt()
                    myPersonalInfo!!.age = age.toInt()
                    myPersonalInfo!!.height = height.toInt()

                    viewLifecycleOwner.lifecycleScope.launch {
                        database.update(myPersonalInfo!!)
                    }

                    Toast.makeText(context, "Update Succeed !", Toast.LENGTH_SHORT).show()
                }

            }
        }


        return binding.root
    }

}


