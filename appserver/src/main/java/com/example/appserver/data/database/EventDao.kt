package com.example.appserver.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.appserver.data.database.entities.ClientEventEntity
import com.example.appserver.data.database.entities.ServerEventEntity
import com.example.appserver.data.database.entities.UseCaseManagerEventEntity
import kotlinx.coroutines.flow.Flow

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
}
