package com.echeng.tally.app.data.dao

import androidx.room.*
import com.echeng.tally.app.data.entity.Counter
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllCounters(): Flow<List<Counter>>

    @Query("SELECT * FROM counters WHERE id = :id")
    suspend fun getCounterById(id: Long): Counter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(counter: Counter): Long

    @Update
    suspend fun update(counter: Counter)

    @Delete
    suspend fun delete(counter: Counter)

    @Query("DELETE FROM counters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM counters")
    suspend fun getNextSortOrder(): Int
}
