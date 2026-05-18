package com.example.telegramreporttool.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val accountId: Long,
    val targetType: String,
    val targetValue: String,
    val category: String,
    val templateId: String,
    val templateDescription: String,
    val evidence: List<String>,
    val status: String,
    val timestamp: Long
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val userId: Long,
    val phoneNumber: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val isActive: Boolean = false
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val category: String,
    val description: String
)
