package com.example.appserver.data

import com.example.appserver.data.database.EventDao
import com.example.appserver.data.database.entities.ClientEventEntity
import com.example.appserver.data.database.entities.ServerEventEntity
import com.example.appserver.data.database.entities.UseCaseManagerEventEntity
import com.example.appserver.domain.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EventLogger : KoinComponent {

    private val eventDao: EventDao by inject()

    fun logServerEvent(eventType: EventType, details: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            eventDao.insertServerEvent(
                ServerEventEntity(
                    timestamp = Clock.System.now(),
                    eventType = eventType,
                    details = details
                )
            )
        }
    }

    fun logUseCaseManagerEvent(
        clientId: String,
        eventType: EventType,
        details: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            eventDao.insertUseCaseManagerEvent(
                UseCaseManagerEventEntity(
                    timestamp = Clock.System.now(),
                    clientId = clientId,
                    eventType = eventType,
                    details = details
                )
            )
        }
    }

    fun logClientEvent(clientId: String, eventType: EventType, details: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            eventDao.insertClientEvent(
                ClientEventEntity(
                    timestamp = Clock.System.now(),
                    clientId = clientId,
                    eventType = eventType,
                    details = details
                )
            )
        }
    }
}
