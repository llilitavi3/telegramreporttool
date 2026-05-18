package com.example.telegramreporttool.data.repository

import android.util.Log
import com.example.telegramreporttool.data.local.AccountEntity
import com.example.telegramreporttool.data.local.ReportDao
import com.example.telegramreporttool.data.local.ReportEntity
import com.example.telegramreporttool.data.local.TemplateEntity
import com.example.telegramreporttool.data.model.*
import com.example.telegramreporttool.data.telegram.TelegramManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.drinkless.tdlib.TdApi

@OptIn(ExperimentalCoroutinesApi::class)
class ReportRepository(
    private val reportDao: ReportDao,
    private val telegramManager: TelegramManager
) {
    val activeUserId: StateFlow<Long?> = telegramManager.activeUserId

    val allAccounts: Flow<List<Account>> = reportDao.getAllAccounts()
        .map { entities -> entities.map { it.toAccountDomain() } }

    val reportsForActiveAccount: Flow<List<Report>> = activeUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList())
        else reportDao.getReportsForAccount(userId).map { entities ->
            entities.map { it.toReportDomain() }
        }
    }

    val allTemplates: Flow<List<Template>> = reportDao.getAllTemplates()
        .map { entities -> entities.map { it.toTemplateDomain() } }

    suspend fun submitReport(report: Report): Boolean {
        val currentUserId = activeUserId.value ?: return false
        Log.d("ReportRepo", "Submitting report for target: ${report.target.value}")
        return try {
            val targetInfo = resolveTarget(report.target.value, currentUserId)
            val chatId = targetInfo.chatId
            var messageIds = targetInfo.messageIds
            
            Log.d("ReportRepo", "Resolved chatId: $chatId, initialMessageIds: ${messageIds.size}")
            
            // If it's a group or channel and we don't have message IDs, try to fetch some messages as proof
            // Telegram usually requires proof for groups/channels
            if (messageIds.isEmpty() && chatId < 0) {
                try {
                    Log.d("ReportRepo", "Fetching messages for proof for chatId: $chatId")
                    val chatHistory = telegramManager.send(TdApi.GetChatHistory(chatId, 0, 0, 5, false), currentUserId)
                    messageIds = chatHistory.messages.map { it.id }.toLongArray()
                    Log.d("ReportRepo", "Attached ${messageIds.size} messages for proof")
                } catch (e: Exception) {
                    Log.w("ReportRepo", "Could not fetch chat history for proof: ${e.message}")
                }
            }
            
            var currentResult: TdApi.ReportChatResult = telegramManager.send(
                TdApi.ReportChat(chatId, byteArrayOf(), messageIds, report.template.description),
                currentUserId
            )
            Log.d("ReportRepo", "Initial report result: ${currentResult::class.java.simpleName}")

            // Loop to handle nested report options
            var attempts = 0
            while (attempts < 10) {
                attempts++
                when (currentResult) {
                    is TdApi.ReportChatResultOptionRequired -> {
                        val result = currentResult as TdApi.ReportChatResultOptionRequired
                        val optionId = findBestOptionId(result, report)
                        Log.d("ReportRepo", "Option required: '${result.title}'. Picking option of size ${optionId.size}")
                        
                        if (optionId.isEmpty()) break
                        
                        currentResult = telegramManager.send(
                            TdApi.ReportChat(chatId, optionId, messageIds, ""),
                            currentUserId
                        )
                    }
                    is TdApi.ReportChatResultTextRequired -> {
                        val result = currentResult as TdApi.ReportChatResultTextRequired
                        Log.d("ReportRepo", "Text required. Sending description: ${report.template.description}")
                        currentResult = telegramManager.send(
                            TdApi.ReportChat(chatId, result.optionId, messageIds, report.template.description),
                            currentUserId
                        )
                    }
                    is TdApi.ReportChatResultMessagesRequired -> {
                        Log.d("ReportRepo", "Messages required by server. Fetching history.")
                        val chatHistory = telegramManager.send(TdApi.GetChatHistory(chatId, 0, 0, 20, false), currentUserId)
                        val fetchedIds = chatHistory.messages.map { it.id }.toLongArray()
                        
                        currentResult = telegramManager.send(
                            TdApi.ReportChat(chatId, byteArrayOf(), fetchedIds, report.template.description),
                            currentUserId
                        )
                    }
                    is TdApi.ReportChatResultOk -> {
                        Log.d("ReportRepo", "Final Result: OK")
                        break
                    }
                    else -> {
                        Log.d("ReportRepo", "Finished with result: ${currentResult::class.java.simpleName}")
                        break
                    }
                }
                Log.d("ReportRepo", "Next step result: ${currentResult::class.java.simpleName}")
            }
            
            val isSuccess = currentResult is TdApi.ReportChatResultOk
            val status = if (isSuccess) ReportStatus.SENT else ReportStatus.FAILED
            
            reportDao.insertReport(report.copy(status = status, accountId = currentUserId).toEntity())
            isSuccess
        } catch (e: Exception) {
            Log.e("ReportRepo", "Report submission failed: ${e.message}")
            reportDao.insertReport(report.copy(status = ReportStatus.FAILED, accountId = currentUserId).toEntity())
            false
        }
    }

    private data class TargetInfo(val chatId: Long, val messageIds: LongArray = longArrayOf())

    private fun findBestOptionId(result: TdApi.ReportChatResultOptionRequired, report: Report): ByteArray {
        val category = report.template.category.lowercase()
        val description = report.template.description.lowercase()
        
        // Try to match category name to option text
        val bestMatch = result.options.find { option ->
            val optText = option.text.lowercase()
            category.contains(optText) || 
            optText.contains(category.substringBefore(" ")) ||
            description.contains(optText)
        }
        
        return bestMatch?.id ?: result.options.firstOrNull()?.id ?: byteArrayOf()
    }

    private suspend fun resolveTarget(target: String, userId: Long): TargetInfo {
        Log.d("ReportRepo", "Resolving target: '$target'")
        val cleanTarget = target.trim()
        
        // 1. Try GetMessageLinkInfo first as it's the most reliable for t.me links
        val urlToTry = when {
            cleanTarget.startsWith("http") -> cleanTarget
            cleanTarget.contains("t.me/") || cleanTarget.contains("telegram.me/") -> {
                if (cleanTarget.startsWith("t.me") || cleanTarget.startsWith("telegram.me")) "https://$cleanTarget"
                else cleanTarget
            }
            cleanTarget.startsWith("@") -> "https://t.me/${cleanTarget.substring(1)}"
            // If it looks like username/123, it's probably a public link
            cleanTarget.contains("/") && !cleanTarget.startsWith("/") -> "https://t.me/$cleanTarget"
            // Simple username (not a raw ID)
            !cleanTarget.contains("/") && cleanTarget.all { it.isLetterOrDigit() || it == '_' } && cleanTarget.toLongOrNull() == null -> "https://t.me/$cleanTarget"
            else -> null
        }
                       
        if (urlToTry != null) {
            try {
                val linkInfo = telegramManager.send(TdApi.GetMessageLinkInfo(urlToTry), userId)
                if (linkInfo is TdApi.MessageLinkInfo) {
                    val msg = linkInfo.message
                    val messageIds = if (msg != null) longArrayOf(msg.id) else longArrayOf()
                    Log.d("ReportRepo", "Resolved via GetMessageLinkInfo: chatId=${linkInfo.chatId}, hasMsg=${msg != null}")
                    return TargetInfo(linkInfo.chatId, messageIds)
                }
            } catch (e: Exception) {
                Log.w("ReportRepo", "GetMessageLinkInfo failed for $urlToTry: ${e.message}")
            }
        }

        // 2. Manual parsing fallback
        val path = cleanTarget.removePrefix("https://").removePrefix("http://")
            .removePrefix("t.me/").removePrefix("telegram.me/").removePrefix("@")
            .removeSuffix("/")
            
        Log.d("ReportRepo", "Manual resolution path: '$path'")

        // Private chat link (t.me/c/123456/789)
        if (path.startsWith("c/")) {
            val parts = path.substring(2).split("/")
            val cid = parts.getOrNull(0)?.toLongOrNull()
            if (cid != null) {
                // TDLib uses -100 + channel_id for channels/supergroups
                val chatId = if (cid < 0) cid else "-100$cid".toLong()
                return TargetInfo(chatId)
            }
        }

        // Public link with message (username/123)
        if (path.contains("/")) {
            val username = path.substringBefore("/")
            if (username.isNotEmpty() && username.all { it.isLetterOrDigit() || it == '_' } && username.toLongOrNull() == null) {
                try {
                    val chat = telegramManager.send(TdApi.SearchPublicChat(username), userId)
                    return TargetInfo(chat.id)
                } catch (e: Exception) {
                    Log.e("ReportRepo", "SearchPublicChat failed for $username: ${e.message}")
                }
            }
        }

        // Raw ID or simple username
        val rawId = path.toLongOrNull()
        if (rawId != null) {
            return TargetInfo(rawId)
        }

        if (path.isNotEmpty() && !path.contains("/") && path.all { it.isLetterOrDigit() || it == '_' } && path.toLongOrNull() == null) {
            try {
                val chat = telegramManager.send(TdApi.SearchPublicChat(path), userId)
                return TargetInfo(chat.id)
            } catch (e: Exception) {
                Log.e("ReportRepo", "Final SearchPublicChat failed for $path: ${e.message}")
            }
        }

        throw Exception("Could not resolve target '$target'")
    }

    suspend fun switchAccount(userId: Long) {
        reportDao.deactivateAllAccounts()
        reportDao.activateAccount(userId)
        telegramManager.initializeExistingAccount(userId)
    }

    suspend fun logout(userId: Long) {
        telegramManager.logout(userId)
        val accounts = reportDao.getAllAccounts().first()
        val accountToDelete = accounts.find { it.userId == userId }
        if (accountToDelete != null) {
            reportDao.deleteAccount(accountToDelete)
        }
    }

    suspend fun syncAccountsFromDb() {
        val accounts = reportDao.getAllAccounts().first()
        for (account in accounts) {
            telegramManager.initializeExistingAccount(account.userId)
        }
        if (activeUserId.value == null && accounts.isNotEmpty()) {
            switchAccount(accounts[0].userId)
        }
    }

    suspend fun saveAccount(user: TdApi.User, phoneNumber: String) {
        val account = AccountEntity(
            userId = user.id,
            phoneNumber = phoneNumber,
            firstName = user.firstName,
            lastName = user.lastName,
            username = user.usernames?.editableUsername ?: "",
            isActive = true
        )
        reportDao.deactivateAllAccounts()
        reportDao.insertAccount(account)
    }

    suspend fun populateTemplates(templates: List<Template>) {
        reportDao.insertTemplates(templates.map { it.toEntity() })
    }
}

fun AccountEntity.toAccountDomain() = Account(
    userId = userId,
    phoneNumber = phoneNumber,
    firstName = firstName,
    lastName = lastName,
    username = username,
    isActive = isActive
)

fun ReportEntity.toReportDomain() = Report(
    id = id,
    accountId = accountId,
    target = Target(TargetType.valueOf(targetType), targetValue),
    template = Template(templateId, category, templateDescription),
    evidence = evidence,
    status = ReportStatus.valueOf(status),
    timestamp = timestamp
)

fun TemplateEntity.toTemplateDomain() = Template(
    id = id,
    category = category,
    description = description
)

fun Report.toEntity() = ReportEntity(
    id = id,
    accountId = accountId,
    targetType = target.type.name,
    targetValue = target.value,
    templateId = template.id,
    category = template.category,
    templateDescription = template.description,
    evidence = evidence,
    status = status.name,
    timestamp = timestamp
)

fun Template.toEntity() = TemplateEntity(
    id = id,
    category = category,
    description = description
)
