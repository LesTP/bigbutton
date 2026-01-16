package com.example.bigbutton.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CompletionEvent::class,
        FinalizedDay::class,
        TrackingMetadata::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BigButtonDatabase : RoomDatabase() {

    abstract fun bigButtonDao(): BigButtonDao

    companion object {
        @Volatile
        private var INSTANCE: BigButtonDatabase? = null

        fun getDatabase(context: Context): BigButtonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BigButtonDatabase::class.java,
                    "bigbutton_history"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
