package com.example.settings

import com.example.settings.SettingsRepository

class FakeSettingsRepository : SettingsRepository {
    override fun getIpAddress(): String = "000.000.000.000"

    override fun getServerPort(): Int = 8888

    override fun saveClientSettings(ipAddress: String, port: Int) {}

    override fun saveServerSettings(port: Int) {}
}