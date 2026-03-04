package com.echeng.tally.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counter_entries",
    foreignKeys = [
        ForeignKey(
            entity = Counter::class,
            parentColumns = ["id"],
            childColumns = ["counterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["counterId", "date"], unique = true),
        Index(value = ["counterId"])
    ]
)
data class CounterEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val counterId: Long,
    val date: String, // ISO "YYYY-MM-DD" local tz
    val count: Int = 0
)
