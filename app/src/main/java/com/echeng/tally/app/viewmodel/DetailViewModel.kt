package com.echeng.tally.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echeng.tally.app.data.db.AppDatabase
import com.echeng.tally.app.data.entity.Counter
import com.echeng.tally.app.data.entity.CounterEntry
import com.echeng.tally.app.data.repository.CounterRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DetailViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val repo = CounterRepository(db.counterDao(), db.counterEntryDao())
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Emits today's date string, re-emits at midnight. */
    private fun todayFlow(): Flow<String> = flow {
        while (true) {
            emit(LocalDate.now().format(dateFormatter))
            val now = LocalDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(now, midnight).toMillis() + 100L)
        }
    }

    private val _counter = MutableStateFlow<Counter?>(null)
    val counter: StateFlow<Counter?> = _counter.asStateFlow()

    private val _entries = MutableStateFlow<List<CounterEntry>>(emptyList())
    val entries: StateFlow<List<CounterEntry>> = _entries.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount.asStateFlow()

    fun loadCounter(counterId: Long) {
        viewModelScope.launch {
            _counter.value = repo.getCounter(counterId)
            // Combine entries with reactive date so todayCount resets at midnight
            combine(
                repo.getEntries(counterId),
                todayFlow()
            ) { entryList, today ->
                _entries.value = entryList
                val entriesSum = entryList.sumOf { it.count }
                val starting = _counter.value?.startingCount ?: 0
                _totalCount.value = starting + entriesSum
                _todayCount.value = entryList.find { it.date == today }?.count ?: 0
            }.collect()
        }
    }

    fun increment() {
        val c = _counter.value ?: return
        _todayCount.update { it + c.stepValue }
        _totalCount.update { it + c.stepValue }
        viewModelScope.launch { repo.incrementToday(c.id, c.stepValue) }
    }

    fun decrement() {
        val c = _counter.value ?: return
        val step = c.stepValue
        _todayCount.update { maxOf(0, it - step) }
        _totalCount.update { maxOf(c.startingCount, it - step) }
        viewModelScope.launch { repo.decrementToday(c.id, step) }
    }

    fun updateEntry(entry: CounterEntry, newCount: Int) {
        val oldCount = entry.count
        val diff = newCount - oldCount
        _totalCount.update { it + diff }
        val today = LocalDate.now().format(dateFormatter)
        if (entry.date == today) {
            _todayCount.value = newCount
        }
        viewModelScope.launch {
            repo.setEntryCount(entry.counterId, entry.date, newCount)
        }
    }
}
