package com.example.settings

interface SettingsRepository {
    fun getIpAddress(): String?
    fun getServerPort(): Int
    fun saveSettings(ipAddress: String, port: Int)
}