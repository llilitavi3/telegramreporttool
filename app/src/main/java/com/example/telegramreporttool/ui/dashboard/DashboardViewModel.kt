package com.example.telegramreporttool.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telegramreporttool.data.model.Account
import com.example.telegramreporttool.data.model.Report
import com.example.telegramreporttool.data.repository.ReportRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    val reports: StateFlow<List<Report>> = repository.reportsForActiveAccount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun switchAccount(userId: Long) {
        viewModelScope.launch {
            repository.switchAccount(userId)
        }
    }

    fun logout(userId: Long) {
        viewModelScope.launch {
            repository.logout(userId)
        }
    }
}
