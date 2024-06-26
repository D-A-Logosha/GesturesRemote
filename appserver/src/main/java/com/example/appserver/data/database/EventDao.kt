package com.example.appserver.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.appserver.data.database.entities.ClientEventEntity
import com.example.appserver.data.database.entities.ServerEventEntity
import com.example.appserver.data.database.entities.UseCaseManagerEventEntity
import com.example.appserver.domain.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface EventDao {

    @Insert
    suspend fun insertServerEvent(event: ServerEventEntity)

    @Insert
    suspend fun insertUseCaseManagerEvent(event: UseCaseManagerEventEntity)

    @Insert
    suspend fun insertClientEvent(event: ClientEventEntity)

    @Query("SELECT * FROM server_events ORDER BY timestamp DESC")
    fun getServerEventsFlow(): Flow<List<ServerEventEntity>>

    @Query("SELECT * FROM use_case_manager_events ORDER BY timestamp DESC")
    fun getUseCaseManagerEventsFlow(): Flow<List<UseCaseManagerEventEntity>>

    @Query("SELECT * FROM client_events ORDER BY timestamp DESC")
    fun getClientEventsFlow(): Flow<List<ClientEventEntity>>

    @Query("""
        SELECT * FROM (
            SELECT id, timestamp, event_type as eventType, null as clientId, details
            FROM server_events
            UNION ALL
            SELECT id, timestamp, event_type as eventType, client_id as clientId, details
            FROM use_case_manager_events
            UNION ALL
            SELECT id, timestamp, event_type as eventType, client_id as clientId, details
            FROM client_events
        )
        WHERE timestamp < :lastTimestamp
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun getEventsFlow(lastTimestamp: Instant, limit: Int): Flow<List<Event>>
}
