package com.example.bigbutton.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Key-value store for tracking configuration.
 * Keys: "tracking_start_date", "last_finalized_date"
 */
@Entity(tableName = "tracking_metadata")
data class TrackingMetadata(
    @PrimaryKey val key: String,
    val value: String
) {
    companion object {
        const val KEY_TRACKING_START_DATE = "tracking_start_date"
        const val KEY_LAST_FINALIZED_DATE = "last_finalized_date"
    }
}
