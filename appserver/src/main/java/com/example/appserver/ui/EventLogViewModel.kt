package com.example.appserver.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appserver.data.database.EventDao
import com.example.appserver.domain.Event
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    private var sameEvents = 0

    private val pageSizeForNew = 1

    private var loadNewEventsJob: Job? = null
    private var newestTimestamp = Instant.DISTANT_PAST

    private val mutexUpdateEvents = Mutex()

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
                loadNewEvents()
            }
        }
    }

    fun loadEvents() {
        if (_uiState.value.reachedEnd) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                mutexUpdateEvents.withLock {
                    val limit = pageSize + sameEvents
                    val newEvents = eventDao.getEvents(
                        lastTimestamp = lastTimestamp, limit = limit
                    )
                    val newEventsUI = newEvents.map { formatEventUI(it) }
                    newEvents.lastOrNull()?.let {
                        lastTimestamp = it.timestamp
                    }
                    if (_uiState.value.isLoading || _uiState.value.isRefreshing) {
                        newEvents.firstOrNull()?.timestamp?.let { newestTimestamp = it }
                        if (_uiState.value.scrollMode == EventLogUiState.ScrollMode.AUTO) {
                            loadNewEvents()
                        }
                    }
                    sameEvents = 0
                    withContext(Dispatchers.Main) {
                        newEventsUI.forEach {
                            if (events.contains(it)) sameEvents++
                            else events.add(it)
                        }
                    }
                    _uiState.update {
                        it.copy(
                            events = events,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            reachedEnd = newEvents.size < limit
                        )
                    }
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
                var sameNewEvents = 0
                while (isActive) {
                    if (_uiState.value.scrollMode == EventLogUiState.ScrollMode.AUTO) {
                        delay(99)
                        mutexUpdateEvents.withLock {
                            val newEvents = eventDao.getNewEvents(
                                newestTimestamp, pageSizeForNew + sameNewEvents
                            )
                            if (newEvents.isNotEmpty()) {
                                newestTimestamp = newEvents.last().timestamp
                                val newEventsUI = newEvents.map { formatEventUI(it) }
                                sameNewEvents = 0
                                withContext(Dispatchers.Main) {
                                    newEventsUI.forEach {
                                        if (events.contains(it)) sameNewEvents++
                                        else events.add(0, it)
                                    }
                                }
                                _uiState.update {
                                    it.copy(
                                        events = events, isLoading = false, error = null
                                    )
                                }
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

    private fun formatEventUI(event: Event) = EventUI(
        id = formatID(event),
        timestamp = formatInstant(event.timestamp),
        eventType = event.eventType,
        clientId = event.clientId,
        details = event.details
    )

    private fun formatID(event: Event) = event.id * EventType.entries.size + event.eventType.ordinal

    private fun formatInstant(instant: Instant): String {
        return instant.toString()
    }

    fun refreshEvents() {
        loadNewEventsJob?.cancel()
        loadNewEventsJob = null
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
