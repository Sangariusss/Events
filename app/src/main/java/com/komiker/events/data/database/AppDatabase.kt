package com.komiker.events.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.komiker.events.data.database.dao.TagCategoryDao
import com.komiker.events.data.database.entities.TagCategoryEntity
import com.komiker.events.data.database.converters.RoomTypeConverters
import com.komiker.events.data.database.dao.LocationDao
import com.komiker.events.data.database.entities.LocationEntity

@Database(entities = [TagCategoryEntity::class, LocationEntity::class], version = 2)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tagCategoryDao(): TagCategoryDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}