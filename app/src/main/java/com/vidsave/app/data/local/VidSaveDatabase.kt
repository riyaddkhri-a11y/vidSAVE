package com.vidsave.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vidsave.app.data.model.DownloadItem

@Database(entities = [DownloadItem::class], version = 1, exportSchema = false)
abstract class VidSaveDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: VidSaveDatabase? = null

        fun getInstance(context: Context): VidSaveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VidSaveDatabase::class.java,
                    "vidsave_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
