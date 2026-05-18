package com.example.telegramreporttool.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Target(
    val type: TargetType,
    val value: String
)

enum class TargetType {
    HANDLE, CHANNEL, MESSAGE
}

@JsonClass(generateAdapter = true)
data class Template(
    val id: String,
    val category: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class Report(
    val id: String,
    val accountId: Long,
    val target: Target,
    val template: Template,
    val evidence: List<String>,
    val status: ReportStatus,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class Account(
    val userId: Long,
    val phoneNumber: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val isActive: Boolean
)

enum class ReportStatus {
    PENDING, SENT, FAILED
}
