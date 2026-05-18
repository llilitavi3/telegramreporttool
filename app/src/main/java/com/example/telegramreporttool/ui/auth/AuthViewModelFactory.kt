package com.example.telegramreporttool.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.telegramreporttool.data.repository.ReportRepository
import com.example.telegramreporttool.data.telegram.TelegramManager

class AuthViewModelFactory(
    private val telegramManager: TelegramManager,
    private val repository: ReportRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(telegramManager, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
