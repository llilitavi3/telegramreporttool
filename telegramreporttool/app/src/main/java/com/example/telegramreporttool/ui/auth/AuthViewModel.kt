package com.example.telegramreporttool.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telegramreporttool.data.repository.ReportRepository
import com.example.telegramreporttool.data.telegram.TelegramManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

data class AuthUiState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val password2FA: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val step: AuthStep = AuthStep.PHONE_NUMBER
)

enum class AuthStep {
    PHONE_NUMBER, OTP, TWO_FACTOR, SUCCESS
}

class AuthViewModel(
    private val telegramManager: TelegramManager,
    private val repository: ReportRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        telegramManager.startNewAuth()
        viewModelScope.launch {
            telegramManager.authorizationState.collect { state ->
                when (state) {
                    is TdApi.AuthorizationStateWaitPhoneNumber -> {
                        _uiState.update { it.copy(step = AuthStep.PHONE_NUMBER, isLoading = false) }
                    }
                    is TdApi.AuthorizationStateWaitCode -> {
                        _uiState.update { it.copy(step = AuthStep.OTP, isLoading = false) }
                    }
                    is TdApi.AuthorizationStateWaitPassword -> {
                        _uiState.update { it.copy(step = AuthStep.TWO_FACTOR, isLoading = false) }
                    }
                    is TdApi.AuthorizationStateReady -> {
                        val user = telegramManager.send(TdApi.GetMe()) as TdApi.User
                        repository.saveAccount(user, _uiState.value.phoneNumber)
                        telegramManager.finalizeAuth(user.id)
                        _uiState.update { it.copy(step = AuthStep.SUCCESS, isLoading = false) }
                    }
                }
            }
        }
    }

    fun onPhoneNumberChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value, error = null) }
    }

    fun onOtpChange(value: String) {
        _uiState.update { it.copy(otpCode = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password2FA = value, error = null) }
    }

    fun sendPhoneNumber() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                telegramManager.send(TdApi.SetAuthenticationPhoneNumber(_uiState.value.phoneNumber, null))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun sendOtp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                telegramManager.send(TdApi.CheckAuthenticationCode(_uiState.value.otpCode))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun sendPassword() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                telegramManager.send(TdApi.CheckAuthenticationPassword(_uiState.value.password2FA))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
