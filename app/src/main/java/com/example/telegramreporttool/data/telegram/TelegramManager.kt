package com.example.telegramreporttool.data.telegram

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.Client
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class TelegramManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val clients = ConcurrentHashMap<Long, Client>()
    private var pendingClient: Client? = null
    private var pendingDirName: String? = null
    private var migrationUserId: Long? = null
    
    private val _authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState: StateFlow<TdApi.AuthorizationState?> = _authorizationState.asStateFlow()

    private val _activeUserId = MutableStateFlow<Long?>(null)
    val activeUserId: StateFlow<Long?> = _activeUserId.asStateFlow()

    private val API_ID = 27157163
    private val API_HASH = "e0145db12519b08e1d2f5628e2db18c4"

    init {
        try {
            System.loadLibrary("tdjni")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startNewAuth() {
        _authorizationState.value = null
        pendingClient?.send(TdApi.Close()) {}
        pendingClient = Client.create(AuthUpdateHandler(), null, null)
        pendingDirName = "auth_${System.currentTimeMillis()}"
    }

    fun initializeExistingAccount(userId: Long) {
        if (!clients.containsKey(userId)) {
            val client = Client.create(DefaultUpdateHandler(userId), null, null)
            clients[userId] = client
        }
        _activeUserId.value = userId
    }

    suspend fun <T : TdApi.Object> send(function: TdApi.Function<T>, userId: Long? = null): T = suspendCancellableCoroutine { continuation ->
        val client = if (userId == null) pendingClient else clients[userId]
        
        client?.send(function) { result ->
            if (result is TdApi.Error) {
                continuation.resumeWithException(Exception(result.message))
            } else {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(result as T)
            }
        } ?: continuation.resumeWithException(Exception("Client for ${userId ?: "pending"} not found"))
    }

    private inner class AuthUpdateHandler : Client.ResultHandler {
        override fun onResult(`object`: TdApi.Object) {
            when (`object`) {
                is TdApi.UpdateAuthorizationState -> {
                    _authorizationState.value = `object`.authorizationState
                    if (`object`.authorizationState is TdApi.AuthorizationStateClosed) {
                        handleClosedAuthClient()
                    } else {
                        handleAuthFlow(`object`.authorizationState)
                    }
                }
            }
        }
    }

    private inner class DefaultUpdateHandler(val userId: Long) : Client.ResultHandler {
        override fun onResult(`object`: TdApi.Object) {
            when (`object`) {
                is TdApi.UpdateAuthorizationState -> {
                    if (`object`.authorizationState is TdApi.AuthorizationStateWaitTdlibParameters) {
                        initializeClient(userId, clients[userId]!!)
                    }
                }
            }
        }
    }

    private fun handleAuthFlow(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                scope.launch {
                    val params = createParams(pendingDirName ?: "auth_temp")
                    pendingClient?.send(params) {}
                }
            }
            is TdApi.AuthorizationStateReady -> {
                // Handled by AuthViewModel to include phone number in repository
            }
        }
    }

    private fun initializeClient(userId: Long, client: Client) {
        scope.launch {
            val params = createParams("user_$userId")
            client.send(params) {}
        }
    }

    private fun createParams(dirName: String) = TdApi.SetTdlibParameters().apply {
        apiId = API_ID
        apiHash = API_HASH
        databaseDirectory = File(context.filesDir, "tdlib/$dirName").absolutePath
        useMessageDatabase = true
        deviceModel = "Android"
        systemLanguageCode = "en"
        applicationVersion = "1.0"
    }

    fun logout(userId: Long) {
        scope.launch {
            clients[userId]?.send(TdApi.LogOut()) {
                clients.remove(userId)
                if (_activeUserId.value == userId) {
                    _activeUserId.value = if (clients.isNotEmpty()) clients.keys().nextElement() else null
                }
            }
        }
    }

    fun finalizeAuth(userId: Long) {
        migrationUserId = userId
        scope.launch {
            pendingClient?.send(TdApi.Close()) {}
        }
    }

    private fun handleClosedAuthClient() {
        val userId = migrationUserId
        val oldDirName = pendingDirName
        
        if (userId != null && oldDirName != null) {
            val oldDir = File(context.filesDir, "tdlib/$oldDirName")
            val newDir = File(context.filesDir, "tdlib/user_$userId")
            
            if (oldDir.exists()) {
                if (newDir.exists()) newDir.deleteRecursively()
                oldDir.renameTo(newDir)
            }
            
            migrationUserId = null
            pendingDirName = null
            pendingClient = null
            
            initializeExistingAccount(userId)
        } else {
            pendingClient = null
            pendingDirName = null
        }
    }
}
