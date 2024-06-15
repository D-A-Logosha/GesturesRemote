package com.example.settings

import android.content.Context
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val PREFS_NAME = "gestures_remote_prefs"
private const val KEY_IP_ADDRESS = "ip_address"
private const val KEY_SERVER_PORT = "server_port"

@Single
class SharedPreferencesSettingsRepository: SettingsRepository, KoinComponent {

    private val context: Context by inject()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getIpAddress(): String? = prefs.getString(KEY_IP_ADDRESS, null)

    override fun getServerPort(): Int = prefs.getInt(KEY_SERVER_PORT, 0)

    override fun saveSettings(ipAddress: String, port: Int) {
        prefs.edit().apply {
            putString(KEY_IP_ADDRESS, ipAddress)
            putInt(KEY_SERVER_PORT, port)
            apply()
        }
    }
}