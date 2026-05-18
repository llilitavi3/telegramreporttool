package com.example.telegramreporttool.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxies")
data class ProxyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val server: String,
    val port: Int,
    val type: String, // "SOCKS5", "HTTP", "MTPROTO"
    val username: String? = null,
    val password: String? = null,
    val secret: String? = null,
    val isHttpCustom: Boolean = false,
    val isEnabled: Boolean = false
)
