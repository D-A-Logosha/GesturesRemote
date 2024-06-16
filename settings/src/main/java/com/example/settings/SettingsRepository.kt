package com.example.settings

interface SettingsRepository {
    fun getIpAddress(): String?
    fun getServerPort(): Int
    fun saveClientSettings(ipAddress: String, port: Int)
    fun saveServerSettings(port: Int)
}