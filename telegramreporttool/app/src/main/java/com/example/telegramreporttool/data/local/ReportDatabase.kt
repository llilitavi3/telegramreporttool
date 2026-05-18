package com.example.telegramreporttool.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReportEntity::class, TemplateEntity::class, AccountEntity::class], version = 3, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    companion object {
        const val DATABASE_NAME = "report_database"
    }
}
