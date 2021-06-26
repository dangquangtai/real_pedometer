package com.taivku.pedometer.fragment

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

class getPedometer {
    var step:String =""
    var distance:String =""
    var speed:String=""
    var time:String= ""
    var calo:String=""
    var friend:String=""

}
//@IgnoreExtraProperties
//data class getPedometer(
//    var step:String? = "",
//    var distance:String? ="",
//    var speed:String? ="",
//    var time:String? = "",
//    var calo:String? ="",
//    var friend:String? =""
//)  {
//
//    @Exclude
//    fun toMap(): Map<String, Any?> {
//        return mapOf(
//            "step" to step,
//            "distance" to distance,
//            "speed" to speed,
//            "time" to time,
//            "calo" to calo,
//            "friend" to friend
//
//        )
//    }
//}