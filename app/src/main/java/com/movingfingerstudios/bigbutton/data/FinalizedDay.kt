package com.movingfingerstudios.bigbutton.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the locked-in status for a completed period day.
 * Once a day is finalized, it should never be modified (use INSERT ... ON CONFLICT IGNORE).
 */
@Entity(tableName = "finalized_days")
data class FinalizedDay(
    @PrimaryKey val date: String,  // "2026-01-15" (LocalDate ISO format)
    val completed: Boolean         // true = done (green), false = missed (red)
)
