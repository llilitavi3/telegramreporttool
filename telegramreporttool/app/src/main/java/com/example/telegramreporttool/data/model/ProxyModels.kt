package com.example.telegramreporttool.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Proxy(
    val id: Int = 0,
    val server: String,
    val port: Int,
    val type: ProxyType,
    val isEnabled: Boolean = false
)

sealed class ProxyType {
    @JsonClass(generateAdapter = true)
    data class Socks5(val username: String = "", val password: String = "") : ProxyType()
    
    @JsonClass(generateAdapter = true)
    data class Http(val username: String = "", val password: String = "", val isHttpCustom: Boolean = false) : ProxyType()
    
    @JsonClass(generateAdapter = true)
    data class Mtproto(val secret: String) : ProxyType()
}
