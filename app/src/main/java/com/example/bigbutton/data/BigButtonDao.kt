package com.example.bigbutton.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface BigButtonDao {

    // ===== CompletionEvent =====

    @Insert
    suspend fun insertCompletionEvent(event: CompletionEvent)

    @Query("SELECT * FROM completion_events WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getEventsInRange(startTime: Long, endTime: Long): List<CompletionEvent>

    @Query("DELETE FROM completion_events WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun deleteEventsInRange(startTime: Long, endTime: Long)

    @Query("DELETE FROM completion_events")
    suspend fun deleteAllCompletionEvents()

    // ===== FinalizedDay =====

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFinalizedDay(day: FinalizedDay)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFinalizedDays(days: List<FinalizedDay>)

    @Query("SELECT * FROM finalized_days WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getDaysInRange(startDate: String, endDate: String): List<FinalizedDay>

    @Query("SELECT * FROM finalized_days WHERE date = :date LIMIT 1")
    suspend fun getDay(date: String): FinalizedDay?

    @Query("DELETE FROM finalized_days")
    suspend fun deleteAllFinalizedDays()

    // ===== TrackingMetadata =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: TrackingMetadata)

    @Query("SELECT value FROM tracking_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): String?

    @Query("DELETE FROM tracking_metadata")
    suspend fun deleteAllMetadata()

    // ===== Clear All History =====

    /**
     * Clears all tracking history data in a single transaction.
     * Does not affect widget state or settings (stored in DataStore).
     */
    @Transaction
    suspend fun clearAllHistory() {
        deleteAllCompletionEvents()
        deleteAllFinalizedDays()
        deleteAllMetadata()
    }
}
