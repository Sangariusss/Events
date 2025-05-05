package com.komiker.events.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.komiker.events.data.database.entities.TagCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagCategoryDao {

    @Query("SELECT * FROM tag_categories")
    fun getAllTagCategories(): Flow<List<TagCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<TagCategoryEntity>)

    @Query("DELETE FROM tag_categories")
    suspend fun deleteAll()
}