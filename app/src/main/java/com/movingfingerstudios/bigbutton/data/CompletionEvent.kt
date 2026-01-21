package com.movingfingerstudios.bigbutton.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records when user pressed "Done" on the widget.
 * Used to determine if a period was completed.
 */
@Entity(tableName = "completion_events")
data class CompletionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,      // epoch millis when user pressed "Done"
    val periodDays: Int       // period setting at time of completion (for audit)
)
