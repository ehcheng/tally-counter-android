package com.echeng.tally.app.data.repository

import com.echeng.tally.app.data.dao.CounterDao
import com.echeng.tally.app.data.dao.CounterEntryDao
import com.echeng.tally.app.data.entity.Counter
import com.echeng.tally.app.data.entity.CounterEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CounterRepository(
    private val counterDao: CounterDao,
    private val entryDao: CounterEntryDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val allCounters: Flow<List<Counter>> = counterDao.getAllCounters()

    suspend fun getCounter(id: Long): Counter? = counterDao.getCounterById(id)

    suspend fun createCounter(name: String, icon: String, colorHex: String, stepValue: Int, startingCount: Int = 0, startDate: String? = null): Long {
        val sortOrder = counterDao.getNextSortOrder()
        return counterDao.insert(
            Counter(name = name, icon = icon, colorHex = colorHex, stepValue = stepValue, startingCount = startingCount, startDate = startDate, sortOrder = sortOrder)
        )
    }

    suspend fun updateCounter(counter: Counter) = counterDao.update(counter)

    suspend fun deleteCounter(id: Long) = counterDao.deleteById(id)

    fun getTodayCount(counterId: Long): Flow<Int> = entryDao.getTotalCount(counterId)

    fun getEntries(counterId: Long): Flow<List<CounterEntry>> = entryDao.getEntriesForCounter(counterId)

    fun getEntriesSince(counterId: Long, startDate: String): Flow<List<CounterEntry>> =
        entryDao.getEntriesSince(counterId, startDate)

    suspend fun incrementToday(counterId: Long, step: Int) {
        val today = LocalDate.now().format(dateFormatter)
        val existing = entryDao.getEntry(counterId, today)
        if (existing != null) {
            entryDao.upsert(existing.copy(count = existing.count + step))
        } else {
            entryDao.upsert(CounterEntry(counterId = counterId, date = today, count = step))
        }
    }

    suspend fun decrementToday(counterId: Long, step: Int) {
        val today = LocalDate.now().format(dateFormatter)
        val existing = entryDao.getEntry(counterId, today)
        if (existing != null) {
            entryDao.upsert(existing.copy(count = maxOf(0, existing.count - step)))
        }
    }

    suspend fun setEntryCount(counterId: Long, date: String, count: Int) {
        val existing = entryDao.getEntry(counterId, date)
        if (existing != null) {
            entryDao.upsert(existing.copy(count = maxOf(0, count)))
        } else {
            entryDao.upsert(CounterEntry(counterId = counterId, date = date, count = maxOf(0, count)))
        }
    }

    suspend fun getAllCountersList(): List<Counter> = counterDao.getAllCounters().first()

    suspend fun getAllEntries(): List<CounterEntry> = entryDao.getAllEntries()

    suspend fun importCounters(counters: List<Counter>) {
        counters.forEach { counterDao.insert(it) }
    }

    suspend fun importEntries(entries: List<CounterEntry>) {
        entryDao.insertAll(entries)
    }
}
