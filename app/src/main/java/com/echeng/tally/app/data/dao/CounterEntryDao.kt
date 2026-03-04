package com.echeng.tally.app.data.dao

import androidx.room.*
import com.echeng.tally.app.data.entity.CounterEntry
import kotlinx.coroutines.flow.Flow

/** Lightweight projection for aggregate count queries. */
data class CounterIdCount(val counterId: Long, val count: Int)

@Dao
interface CounterEntryDao {
    @Query("SELECT * FROM counter_entries WHERE counterId = :counterId ORDER BY date DESC")
    fun getEntriesForCounter(counterId: Long): Flow<List<CounterEntry>>

    @Query("SELECT * FROM counter_entries WHERE counterId = :counterId AND date = :date LIMIT 1")
    suspend fun getEntry(counterId: Long, date: String): CounterEntry?

    @Query("SELECT * FROM counter_entries WHERE counterId = :counterId AND date >= :startDate ORDER BY date ASC")
    fun getEntriesSince(counterId: Long, startDate: String): Flow<List<CounterEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CounterEntry)

    @Query("SELECT COALESCE(SUM(count), 0) FROM counter_entries WHERE counterId = :counterId")
    fun getTotalCount(counterId: Long): Flow<Int>

    /** Aggregate entry totals per counter — single query replaces N per-counter lookups. */
    @Query("SELECT counterId, COALESCE(SUM(count), 0) AS count FROM counter_entries GROUP BY counterId")
    fun getTotalCountsByCounter(): Flow<List<CounterIdCount>>

    /** Today's count per counter — single query for all counters on a given date. */
    @Query("SELECT counterId, count FROM counter_entries WHERE date = :date")
    fun getCountsByDate(date: String): Flow<List<CounterIdCount>>

    @Query("SELECT * FROM counter_entries WHERE counterId = :counterId")
    suspend fun getAllEntriesForCounter(counterId: Long): List<CounterEntry>

    @Query("SELECT * FROM counter_entries ORDER BY date DESC")
    suspend fun getAllEntries(): List<CounterEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CounterEntry>)
}
