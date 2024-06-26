package com.example.appserver.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.appserver.data.database.entities.ClientEventEntity
import com.example.appserver.data.database.entities.ServerEventEntity
import com.example.appserver.data.database.entities.UseCaseManagerEventEntity
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

@Database(
    entities = [
        ServerEventEntity::class,
        UseCaseManagerEventEntity::class,
        ClientEventEntity::class
    ],
    version = 1,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {

        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "gestures_remote.db"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

val databaseModule = module {
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().eventDao() }
}
