package com.echeng.tally.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echeng.tally.app.data.db.AppDatabase
import com.echeng.tally.app.data.entity.Counter
import com.echeng.tally.app.data.repository.CounterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val repo = CounterRepository(db.counterDao(), db.counterEntryDao())

    private val _counter = MutableStateFlow<Counter?>(null)
    val counter: StateFlow<Counter?> = _counter

    fun loadCounter(id: Long) {
        viewModelScope.launch {
            _counter.value = repo.getCounter(id)
        }
    }

    fun saveCounter(name: String, icon: String, colorHex: String, stepValue: Int, startingCount: Int, startDate: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            val existing = _counter.value
            if (existing != null) {
                repo.updateCounter(existing.copy(name = name, icon = icon, colorHex = colorHex, stepValue = stepValue, startingCount = startingCount, startDate = startDate))
            } else {
                repo.createCounter(name, icon, colorHex, stepValue, startingCount, startDate)
            }
            onDone()
        }
    }
}
