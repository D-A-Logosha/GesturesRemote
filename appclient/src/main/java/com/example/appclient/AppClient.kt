package com.example.appclient

import android.app.Application
import com.example.appclient.data.accessibility.GestureServiceController
import com.example.appclient.data.websocket.KtorWebSocketClient
import com.example.appclient.data.websocket.WebSocketClient
import com.example.appclient.data.websocket.httpClientModule
import com.example.appclient.domain.interfaces.ChromeSwipeAreaProvider
import com.example.appclient.domain.interfaces.GestureServiceHandler
import com.example.appclient.domain.interfaces.GestureServiceManager
import com.example.appclient.domain.interfaces.PerformedGesturesProvider
import com.example.appclient.domain.usecase.ExecuteGestureUseCase
import com.example.appclient.domain.usecase.ReceiveGestureUseCase
import com.example.appclient.domain.usecase.SendMessageUseCase
import com.example.appclient.domain.usecase.UseCaseManager
import com.example.appclient.ui.ClientViewModel
import com.example.settings.SettingsRepository
import com.example.settings.SharedPreferencesSettingsRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.binds
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
        viewModel {
            this@module.scope(named<ClientViewModel>()) {
                scoped(qualifier = named("viewModelScope")) { getSource<CoroutineScope>() }
                scoped {}
            }
            ClientViewModel(get(), get(), get())
        }
        single<SettingsRepository> { SharedPreferencesSettingsRepository() }
        single<WebSocketClient> { KtorWebSocketClient() }
        single<GestureServiceController> { GestureServiceController() } binds arrayOf(
            GestureServiceHandler::class,
            GestureServiceManager::class,
            ChromeSwipeAreaProvider::class,
            PerformedGesturesProvider::class
        )
        factory { (parentScope: CoroutineScope) -> UseCaseManager(parentScope) }
        factory { (parentScope: CoroutineScope) -> SendMessageUseCase(parentScope) }
        factory { (parentScope: CoroutineScope) -> ReceiveGestureUseCase(parentScope) }
        factory { (parentScope: CoroutineScope) -> ExecuteGestureUseCase(parentScope) }

    } + httpClientModule
}
