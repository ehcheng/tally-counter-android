package com.echeng.tally.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "🔢",
    val colorHex: String = "#00F0FF",
    val stepValue: Int = 1,
    val startingCount: Int = 0,
    val startDate: String? = null, // ISO date (yyyy-MM-dd) for migration
    val targetCount: Int? = null, // optional goal count
    val deadlineDate: String? = null, // ISO date (yyyy-MM-dd) deadline for target
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
