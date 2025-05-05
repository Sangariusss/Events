package com.komiker.events.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.komiker.events.data.database.dao.TagCategoryDao
import com.komiker.events.data.database.entities.TagCategoryEntity
import com.komiker.events.data.database.converters.RoomTypeConverters

@Database(entities = [TagCategoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tagCategoryDao(): TagCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}