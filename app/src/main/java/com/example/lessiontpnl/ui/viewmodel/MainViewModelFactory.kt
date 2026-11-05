package com.example.lessiontpnl.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lessiontpnl.data.repository.MeasurementRepository

class MainViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(MainViewModel::class.java)) {
            throw IllegalArgumentException("ViewModel no soportado: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            context = applicationContext,
            repository = MeasurementRepository(applicationContext)
        ) as T
    }
}
