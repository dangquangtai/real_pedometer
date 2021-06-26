package com.taivku.pedometer.fragment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.taivku.pedometer.database.Pedometer
import com.taivku.pedometer.database.PedometerDatabaseDAO


import kotlinx.coroutines.launch

class DataViewModel(
    val database: PedometerDatabaseDAO,
    application: Application
) : AndroidViewModel(application) {
    var getAllPedometer: LiveData<List<Pedometer>>? = null

    fun onGetData(begin: Long, end: Long) {
        getAllPedometer = database.getBetweenPedometer(begin, end)
    }

    fun onGetToday(date: Long) {
        viewModelScope.launch {
            getAllPedometer = database.getListPedometer(date)
        }
    }
}