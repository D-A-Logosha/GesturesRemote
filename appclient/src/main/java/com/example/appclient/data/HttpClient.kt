package com.example.appclient.data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import org.koin.dsl.module

val httpClientModule = module {
    single {
        HttpClient(CIO) {
            install(WebSockets){
                pingInterval = 20_000
            }
        }
    }
}