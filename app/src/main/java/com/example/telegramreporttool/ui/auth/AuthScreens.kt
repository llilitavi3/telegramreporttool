package com.example.telegramreporttool.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    state: AuthUiState,
    onPhoneNumberChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSendPhone: () -> Unit,
    onSendOtp: () -> Unit,
    onSendPassword: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Telegram Login") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Connect Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Use your official Telegram account to send reports.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            when (state.step) {
                AuthStep.PHONE_NUMBER -> PhoneStep(
                    phoneNumber = state.phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    onNext = onSendPhone,
                    isLoading = state.isLoading,
                    error = state.error
                )
                AuthStep.OTP -> OtpStep(
                    otp = state.otpCode,
                    onValueChange = onOtpChange,
                    onNext = onSendOtp,
                    isLoading = state.isLoading,
                    error = state.error
                )
                AuthStep.TWO_FACTOR -> PasswordStep(
                    password = state.password2FA,
                    onValueChange = onPasswordChange,
                    onNext = onSendPassword,
                    isLoading = state.isLoading,
                    error = state.error
                )
                AuthStep.SUCCESS -> {
                    // Handled by navigation in MainApp
                }
            }
        }
    }
}

@Composable
fun PhoneStep(
    phoneNumber: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onValueChange,
            label = { Text("Phone Number") },
            placeholder = { Text("+1234567890") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
            isError = error != null,
            supportingText = { error?.let { Text(it) } }
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneNumber.isNotBlank() && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("Send Verification Code")
        }
    }
}

@Composable
fun OtpStep(
    otp: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = otp,
            onValueChange = onValueChange,
            label = { Text("OTP Code") },
            placeholder = { Text("12345") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = null) },
            isError = error != null,
            supportingText = { error?.let { Text(it) } }
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = otp.isNotBlank() && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("Verify Code")
        }
    }
}

@Composable
fun PasswordStep(
    password: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = password,
            onValueChange = onValueChange,
            label = { Text("2FA Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            isError = error != null,
            supportingText = { error?.let { Text(it) } }
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = password.isNotBlank() && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("Login")
        }
    }
}
