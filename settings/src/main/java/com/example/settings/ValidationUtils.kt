package com.example.settings

object ValidationUtils {

    fun isValidIpAddress(ipAddress: String): Boolean {
        val pattern = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$")
        return pattern.matches(ipAddress)
    }

    fun isValidPort(port: String): Boolean {
        val portNumber = port.toIntOrNull()
        return portNumber != null && portNumber in 1..65535
    }
}