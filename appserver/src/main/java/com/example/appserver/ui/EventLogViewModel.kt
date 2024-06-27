package com.example.appserver.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appserver.data.database.EventDao
import com.example.appserver.domain.EventType
import com.example.appserver.ui.EventLogUiState.EventUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class EventLogUiState(
    val events: List<EventUI> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val reachedEnd: Boolean = false,
) {
    data class EventUI(
        val id: Int,
        val timestamp: String,
        val eventType: EventType,
        val clientId: String? = null,
        val details: String? = null
    )
}

class EventLogViewModel(
    private val eventDao: EventDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventLogUiState(isLoading = true))
    val uiState: StateFlow<EventLogUiState> = _uiState.asStateFlow()

    private val events = mutableStateListOf<EventUI>()
    private var lastTimestamp: Instant = Instant.DISTANT_FUTURE
    private val pageSize = 10
    private val pageSizeForNew = 1
    
    private var loadNewEventsJob: Job? = null

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val newEvents = eventDao.getEvents(
                    lastTimestamp = lastTimestamp, limit = pageSize
                )
                val newEventsUI = newEvents.map { event ->
                    EventUI(
                        id = event.id,
                        timestamp = formatInstant(event.timestamp),
                        eventType = event.eventType,
                        clientId = event.clientId,
                        details = event.details
                    )
                }
                newEvents.lastOrNull()?.let {
                    lastTimestamp = it.timestamp
                }
                if (newEvents.size < pageSize) _uiState.update {
                    it.copy(reachedEnd = true)
                }
                events.addAll(newEventsUI)
                if (_uiState.value.isLoading) loadNewEvents(
                    newEvents.firstOrNull()?.timestamp ?: Instant.DISTANT_PAST
                )
                _uiState.update {
                    it.copy(
                        events = events,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = "EventLogViewModel: " + e.message, isLoading = false)
                }
            }
        }
    }

    private fun loadNewEvents(newestTimestamp: Instant) {
        loadNewEventsJob?.let { return }
        var newestTimestampJob = newestTimestamp
        loadNewEventsJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                while (isActive) {
                    delay(99)
                    val newEvents = eventDao.getNewEvents(newestTimestampJob, pageSizeForNew)
                    if (newEvents.isNotEmpty()) {
                        newestTimestampJob = newEvents.first().timestamp
                        val newEventsUI = newEvents.map { event ->
                            EventUI(
                                id = event.id,
                                timestamp = formatInstant(event.timestamp),
                                eventType = event.eventType,
                                clientId = event.clientId,
                                details = event.details
                            )
                        }
                        newEventsUI.forEach {
                            events.add(0, it)
                        }
                        _uiState.update {
                            it.copy(
                                events = events, isLoading = false, error = null
                            )
                        }
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = "EventLogViewModel: " + e.message, isLoading = false)
                }
            }
        }
    }


    private fun formatInstant(instant: Instant): String {
        return instant.toString()
    }

    fun refreshEvents() {
        _uiState.update {
            it.copy(isRefreshing = true, reachedEnd = false)
        }
        lastTimestamp = Instant.DISTANT_FUTURE
        events.clear()
        loadEvents()
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
