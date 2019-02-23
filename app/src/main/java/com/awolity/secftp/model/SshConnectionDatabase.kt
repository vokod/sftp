package com.awolity.secftp.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SshConnectionData::class], version = 2, exportSchema = false)
abstract class SshConnectionDatabase : RoomDatabase() {

    abstract fun connectionDao(): SshConnectionDataDao

    companion object {

        @Volatile private var instance: SshConnectionDatabase? = null

        fun getInstance(context: Context): SshConnectionDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SshConnectionDatabase {
            return Room.databaseBuilder(context, SshConnectionDatabase::class.java, "connection.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}