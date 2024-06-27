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
    val scrollMode: ScrollMode = ScrollMode.AUTO,
) {
    data class EventUI(
        val id: Int,
        val timestamp: String,
        val eventType: EventType,
        val clientId: String? = null,
        val details: String? = null
    )

    enum class ScrollMode {
        AUTO, MANUAL
    }
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
    private var newestTimestamp = Instant.DISTANT_PAST

    init {
        loadEvents()
    }

    fun toggleScrollMode() {
        when (_uiState.value.scrollMode) {
            EventLogUiState.ScrollMode.AUTO -> {
                _uiState.update { it.copy(scrollMode = EventLogUiState.ScrollMode.MANUAL) }
            }

            EventLogUiState.ScrollMode.MANUAL -> {
                _uiState.update { it.copy(scrollMode = EventLogUiState.ScrollMode.AUTO) }
                loadNewEventsJob?.let {
                    loadNewEvents()
                }
            }
        }
    }

    fun loadEvents() {
        if (_uiState.value.reachedEnd) return
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
                events.addAll(newEventsUI)
                if (_uiState.value.isLoading && _uiState.value.scrollMode == EventLogUiState.ScrollMode.AUTO) {
                    newEvents.firstOrNull()?.timestamp?.let { newestTimestamp = it }
                    loadNewEvents()
                }
                _uiState.update {
                    it.copy(
                        events = events,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        reachedEnd = newEvents.size < pageSize
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = "EventLogViewModel: " + e.message, isLoading = false)
                }
            }
        }
    }

    private fun loadNewEvents() {
        loadNewEventsJob?.let { return }
        loadNewEventsJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                while (isActive) {
                    if (_uiState.value.scrollMode == EventLogUiState.ScrollMode.AUTO) {
                        delay(99)
                        val newEvents = eventDao.getNewEvents(newestTimestamp, pageSizeForNew)
                        if (newEvents.isNotEmpty()) {
                            newestTimestamp = newEvents.first().timestamp
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
                    } else {
                        delay(256)
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = "EventLogViewModel: " + e.message, isLoading = false)
                }
                loadNewEventsJob?.cancel()
                loadNewEventsJob = null
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
        newestTimestamp = Instant.DISTANT_PAST
        events.clear()
        loadEvents()
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
