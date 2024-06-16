package com.example.appserver

import android.app.Application
import com.example.appserver.data.KtorWebSocketServer
import com.example.appserver.data.WebSocketServer
import com.example.appserver.ui.ServerViewModel
import com.example.settings.SettingsRepository
import com.example.settings.SharedPreferencesSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AppServer : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@AppServer)
            modules(appModule)
        }
    }

    private val appModule = module {
        viewModel { ServerViewModel(get(), get()) }
        single<SettingsRepository> { SharedPreferencesSettingsRepository() }
        single<WebSocketServer> { KtorWebSocketServer() }
    }
}