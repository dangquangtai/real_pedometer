package com.taivku.pedometer.activity

import android.content.ClipboardManager
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.taivku.pedometer.R
import com.taivku.pedometer.fragment.getPedometer
import kotlinx.android.synthetic.main.activity_race.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*


class RaceActivity : AppCompatActivity() {
    var friendId = ""
    var isClick = false
    var userValue: Double = 0.0
    var friendValue: Double = 0.0
    var userCaloValue: Float = 0f
    var friendCaloValue: Float = 0f
//  private  var homeFragment = HomeFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race)
        val date = Date()
        val strDateFormat = "dd_MM_yyyy"
        val sdf = SimpleDateFormat(strDateFormat)
        val todayDate = sdf.format(date)
        val database = Firebase.database
        val mDatabase = database.getReference()
        val prefsChom = applicationContext?.getSharedPreferences("prefsDay", Context.MODE_PRIVATE)
        var getKey = prefsChom!!.getString("getDay", null)
        if (getKey != null) {
            display_user.text = getKey.toString()
        }
        val myTopPostsQuery = mDatabase.child(todayDate).child("users").child(getKey.toString())
        myTopPostsQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val pedometer1 = dataSnapshot.getValue<getPedometer>()
                distanceMap.text = BigDecimal(pedometer1?.distance!!.toDouble()).setScale(
                    2,
                    RoundingMode.HALF_EVEN
                )
                    .toString()
                stepMap.text = BigDecimal(pedometer1?.step!!.toDouble()).setScale(
                    0,
                    RoundingMode.HALF_EVEN
                )
                    .toString()

                timeMap.setBase(SystemClock.elapsedRealtime() -  pedometer1?.time.toLong())
//                timeMap.text = pedometer1?.time.toString()
                speedMap.text =
                    BigDecimal(pedometer1?.speed!!.toDouble()).setScale(2, RoundingMode.HALF_EVEN)
                        .toString()
                caloMap.text =
                    BigDecimal(pedometer1?.calo!!.toDouble()).setScale(2, RoundingMode.HALF_EVEN)
                        .toString()
                userValue = pedometer1?.distance.toDouble()
                userCaloValue = pedometer1?.calo.toFloat()

                if (pedometer1?.friend != "0") {
                    val prefsChom1 = applicationContext?.getSharedPreferences(
                        "prefsFriend",
                        Context.MODE_PRIVATE
                    )
                    val editors = prefsChom1!!.edit()
                    editors.putString("friendId", pedometer1?.friend)
                    editors.apply()

                } else {
                    Toast.makeText(applicationContext, "No friend matched !", Toast.LENGTH_SHORT).show()
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        })
        btn_findFriend.setOnClickListener {
            var friend_text = search_friend.text.toString()
            isClick = true
            val prefsChom =
                applicationContext?.getSharedPreferences("prefsDay", Context.MODE_PRIVATE)
            var getId = prefsChom!!.getString("getDay", null)
            val prefsId =
                applicationContext?.getSharedPreferences("prefsFriend", Context.MODE_PRIVATE)
            var getIdFriend = prefsId!!.getString("friendId", null)

            if (TextUtils.isEmpty(friend_text)) {
                Toast.makeText(applicationContext, "Please enter data !", Toast.LENGTH_SHORT)
                    .show()
            } else if (getIdFriend != null) {
                Toast.makeText(
                    applicationContext,
                    "Please cancel match before matched again !",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {

                val myTopPostsQueryFriend =
                    mDatabase.child(todayDate).child("users").child(friend_text)
                myTopPostsQueryFriend.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val pedometer = dataSnapshot.getValue<getPedometer>()
                        if (pedometer != null && isClick) {


                            mDatabase.child(todayDate).child("users").child(getId.toString())
                                .child("friend")
                                .setValue(friend_text)
                            mDatabase.child(todayDate).child("users").child(friend_text)
                                .child("friend")
                                .setValue(getId)
                            isClick = false


                        } else {
                            Toast.makeText(applicationContext, "Your friend may not online today !", Toast.LENGTH_SHORT).show()
                        }


                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                        // ...
                    }
                })
            }

        }
        copyUser.setOnClickListener(View.OnClickListener {
            val cm: ClipboardManager =
                applicationContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setText(display_user.getText())
            Toast.makeText(applicationContext, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        })

        btnDelete.setOnClickListener(View.OnClickListener {
            val prefsChom2 =
                applicationContext?.getSharedPreferences("prefsFriend", Context.MODE_PRIVATE)
            var getFriendId = prefsChom2!!.getString("friendId", null)
            mDatabase.child(todayDate).child("users").child(getKey.toString())
                .child("friend")
                .setValue("0")
            mDatabase.child(todayDate).child("users").child(getFriendId.toString())
                .child("friend")
                .setValue("0")
            val prefsChom5 =
                applicationContext?.getSharedPreferences("prefsFriend", Context.MODE_PRIVATE)
            val editors = prefsChom5!!.edit()
            editors.clear()
            editors.commit()
        })
        backBtn.setOnClickListener(View.OnClickListener {
            this.finish()

        })


        lifecycleScope.launch {
            while (true) {
                val prefsChom3 =
                    applicationContext?.getSharedPreferences("prefsFriend", Context.MODE_PRIVATE)
                var getFriendid1 = prefsChom3!!.getString("friendId", null)
                if (TextUtils.isEmpty(getFriendid1)) {
                    distanceMap2.text = "00.00"
                    stepMap2.text = "0"
                    timeMap2.text = "00:00"
                    speedMap2.text = "00,00"
                    caloMap2.text = "00,00"
                    displayFriend.text = "There's no friend matched !"


                } else {
                    val myTopPostsQueryFriendrun =
                        mDatabase.child(todayDate).child("users").child(getFriendid1.toString())
                    myTopPostsQueryFriendrun.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val pedometer = dataSnapshot.getValue<getPedometer>()
                            if (pedometer != null) {

                                distanceMap2.text =
                                    BigDecimal(pedometer?.distance!!.toDouble()).setScale(
                                        2,
                                        RoundingMode.HALF_EVEN
                                    )
                                        .toString()
                                stepMap2.text = BigDecimal(pedometer?.step!!.toDouble()).setScale(
                                    0,
                                    RoundingMode.HALF_EVEN
                                )
                                    .toString()
                                timeMap2.setBase(SystemClock.elapsedRealtime() - pedometer?.time.toLong())
//                                timeMap2.text = pedometer?.time.toString()
                                speedMap2.text = BigDecimal(pedometer?.speed!!.toDouble()).setScale(
                                    2,
                                    RoundingMode.HALF_EVEN
                                )
                                    .toString()
                                caloMap2.text = BigDecimal(pedometer?.calo!!.toDouble()).setScale(
                                    2,
                                    RoundingMode.HALF_EVEN
                                )
                                    .toString()

                                displayFriend.text = "Your friend id:" + "  " + getFriendid1
                                friendValue = pedometer?.distance.toDouble()
                                if (userValue < friendValue) {
                                    btnWin2.isInvisible = false
                                    btnWin.isInvisible = true
                                } else if (userValue == friendValue) {
                                    if (userCaloValue < friendCaloValue) {
                                        btnWin2.isInvisible = false
                                        btnWin.isInvisible = true
                                    } else {
                                        btnWin2.isInvisible = true
                                        btnWin.isInvisible = false
                                    }
                                } else {
                                    btnWin2.isInvisible = true
                                    btnWin.isInvisible = false
                                }

                            }


                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Getting Post failed, log a message
                            Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                            // ...
                        }
                    })
                }
                delay(1500)
            }
        }


//        Toast.makeText(applicationContext, friendId, Toast.LENGTH_SHORT).show()

    }


}


