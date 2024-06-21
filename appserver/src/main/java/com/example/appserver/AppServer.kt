package com.example.appserver

import android.app.Application
import com.example.appserver.data.KtorWebSocketServer
import com.example.appserver.data.WebSocketServer
import com.example.appserver.domain.usecase.GenerateGestureDataUseCase
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
        single<SettingsRepository> { SharedPreferencesSettingsRepository() }
        single<WebSocketServer> { KtorWebSocketServer() }
        factory { (parentScope: CoroutineScope) -> GenerateGestureDataUseCase(parentScope) }
    }
}
