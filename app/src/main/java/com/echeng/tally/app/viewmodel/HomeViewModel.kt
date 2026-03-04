package com.echeng.tally.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echeng.tally.app.data.db.AppDatabase
import com.echeng.tally.app.data.entity.Counter
import com.echeng.tally.app.data.repository.CounterRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val repo = CounterRepository(db.counterDao(), db.counterEntryDao())
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val counters: Flow<List<Counter>> = repo.allCounters

    private val _todayCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val todayCounts: StateFlow<Map<Long, Int>> = _todayCounts.asStateFlow()

    private val _totalCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val totalCounts: StateFlow<Map<Long, Int>> = _totalCounts.asStateFlow()

    init {
        val today = LocalDate.now().format(dateFormatter)

        // Single reactive flow for all today counts (replaces N per-counter queries)
        viewModelScope.launch {
            db.counterEntryDao().getCountsByDate(today).collect { entries ->
                _todayCounts.value = entries.associate { it.counterId to it.count }
            }
        }

        // Combine counters + aggregate entry totals → total counts map
        // (replaces N per-counter getAllEntriesForCounter calls)
        viewModelScope.launch {
            combine(
                repo.allCounters,
                db.counterEntryDao().getTotalCountsByCounter()
            ) { counterList, entryTotals ->
                val entryMap = entryTotals.associate { it.counterId to it.count }
                counterList.associate { c -> c.id to (c.startingCount + (entryMap[c.id] ?: 0)) }
            }.collect { _totalCounts.value = it }
        }
    }

    fun increment(counter: Counter) {
        _todayCounts.update { it + (counter.id to (it[counter.id] ?: 0) + counter.stepValue) }
        _totalCounts.update { it + (counter.id to (it[counter.id] ?: 0) + counter.stepValue) }
        viewModelScope.launch { repo.incrementToday(counter.id, counter.stepValue) }
    }

    fun decrement(counter: Counter) {
        val currentToday = _todayCounts.value[counter.id] ?: 0
        val newToday = maxOf(0, currentToday - counter.stepValue)
        val diff = currentToday - newToday
        _todayCounts.update { it + (counter.id to newToday) }
        _totalCounts.update { it + (counter.id to maxOf(counter.startingCount, (it[counter.id] ?: 0) - diff)) }
        viewModelScope.launch { repo.decrementToday(counter.id, counter.stepValue) }
    }

    fun moveCounterUp(counterId: Long) {
        viewModelScope.launch {
            val currentList = counters.first()
            val idx = currentList.indexOfFirst { it.id == counterId }
            if (idx <= 0) return@launch
            val newList = currentList.toMutableList()
            java.util.Collections.swap(newList, idx, idx - 1)
            reorderCounters(newList)
        }
    }

    fun moveCounterDown(counterId: Long) {
        viewModelScope.launch {
            val currentList = counters.first()
            val idx = currentList.indexOfFirst { it.id == counterId }
            if (idx < 0 || idx >= currentList.size - 1) return@launch
            val newList = currentList.toMutableList()
            java.util.Collections.swap(newList, idx, idx + 1)
            reorderCounters(newList)
        }
    }

    fun reorderCounters(reordered: List<Counter>) {
        viewModelScope.launch {
            reordered.forEachIndexed { index, counter ->
                repo.updateCounter(counter.copy(sortOrder = index))
            }
        }
    }

    fun deleteCounter(counter: Counter) {
        viewModelScope.launch {
            repo.deleteCounter(counter.id)
            _todayCounts.update { it - counter.id }
            _totalCounts.update { it - counter.id }
        }
    }
}
