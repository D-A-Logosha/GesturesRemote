package com.example.appserver.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appserver.data.database.EventDao
import com.example.appserver.domain.EventType
import com.example.appserver.ui.EventLogUiState.EventUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class EventLogUiState(
    val events: List<EventUI> = emptyList(),
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val reachedEnd: Boolean = false,
) {
    data class EventUI(
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
    private var lastTimestamp: Instant? = null
    val pageSize = 10

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            eventDao.getEventsFlow(lastTimestamp ?: Instant.DISTANT_FUTURE, pageSize)
                .map { events ->
                    events.map { event ->
                        EventUI(
                            timestamp = formatInstant(event.timestamp),
                            eventType = event.eventType,
                            clientId = event.clientId,
                            details = event.details
                        )
                    }.also {
                        lastTimestamp = events.lastOrNull()?.timestamp
                        if (events.size < pageSize) _uiState.update {
                            it.copy(reachedEnd = true)
                        }
                    }

                }
                .catch { e ->
                    _uiState.update {
                        it.copy(error = e, isLoading = false)
                    }
                }
                .collect { eventsUI ->
                    this@EventLogViewModel.events.addAll(eventsUI)
                    _uiState.update {
                        it.copy(
                            events = this@EventLogViewModel.events,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun formatInstant(instant: Instant): String {
        return instant.toString()
    }

    fun refreshEvents() {
        lastTimestamp = null
        _uiState.update { EventLogUiState(isLoading = true) }
        loadEvents()
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
