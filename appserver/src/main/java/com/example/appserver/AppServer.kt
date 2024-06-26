package com.example.appserver

import android.app.Application
import com.example.appserver.data.EventLogger
import com.example.appserver.data.database.databaseModule
import com.example.appserver.data.websocket.KtorWebSocketServer
import com.example.appserver.data.websocket.WebSocketServer
import com.example.appserver.ui.EventLogViewModel
import com.example.appserver.ui.ServerViewModel
import com.example.settings.SettingsRepository
import com.example.settings.SharedPreferencesSettingsRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
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
        viewModel {
            this@module.scope(named<ServerViewModel>()) {
                scoped(qualifier = named("viewModelScope")) { getSource<CoroutineScope>() }
            }
            ServerViewModel(get(), get())
        }
        viewModel { EventLogViewModel(get()) }
        single<SettingsRepository> { SharedPreferencesSettingsRepository() }
        single<WebSocketServer> { KtorWebSocketServer() }
        single { EventLogger() }
    } + databaseModule
}
