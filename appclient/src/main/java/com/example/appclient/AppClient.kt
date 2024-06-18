package com.example.appclient

import android.app.Application
import com.example.appclient.data.websocket.KtorWebSocketClient
import com.example.appclient.data.websocket.WebSocketClient
import com.example.appclient.data.websocket.httpClientModule
import com.example.appclient.ui.ClientViewModel
import com.example.settings.SettingsRepository
import com.example.settings.SharedPreferencesSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AppClient : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@AppClient)
            modules(appModule)
        }
    }

    private val appModule = module {
        viewModel { ClientViewModel(get(), get()) }
        single<SettingsRepository> { SharedPreferencesSettingsRepository() }
        single<WebSocketClient> { KtorWebSocketClient() }
    } + httpClientModule

}