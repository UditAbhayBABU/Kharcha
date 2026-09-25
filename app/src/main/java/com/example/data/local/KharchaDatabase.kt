package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        BusinessEntity::class,
        PotEntity::class,
        UdhaarPartyEntity::class,
        UdhaarEntryEntity::class,
        RecurringExpenseEntity::class,
        CategoryBudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KharchaDatabase : RoomDatabase() {
    abstract fun kharchaDao(): KharchaDao

    companion object {
        @Volatile
        private var INSTANCE: KharchaDatabase? = null

        fun getInstance(context: Context): KharchaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KharchaDatabase::class.java,
                    "kharcha_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
