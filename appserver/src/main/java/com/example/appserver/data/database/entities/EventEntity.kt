package com.example.appserver.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.appserver.domain.EventType
import kotlinx.datetime.Instant

@Entity(tableName = "server_events")
data class ServerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
    @ColumnInfo(name = "event_type") val eventType: EventType,
    @ColumnInfo(name = "details") val details: String? = null
)

@Entity(tableName = "use_case_manager_events")
data class UseCaseManagerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "event_type") val eventType: EventType,
    @ColumnInfo(name = "details") val details: String? = null
)

@Entity(tableName = "client_events")
data class ClientEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "event_type") val eventType: EventType,
    @ColumnInfo(name = "details") val details: String? = null
)
