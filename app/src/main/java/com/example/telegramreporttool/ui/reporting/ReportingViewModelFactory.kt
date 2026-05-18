package com.example.telegramreporttool.ui.reporting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.telegramreporttool.data.repository.ReportRepository

class ReportingViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
