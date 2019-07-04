package com.awolity.secftp.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "connection_table")
data class SshConnectionData(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "address") var address: String,
    @ColumnInfo(name = "username") var username: String,
    @ColumnInfo(name = "port") var port: Int,
    @ColumnInfo(name = "auth_method") var authMethod: Int,
    @ColumnInfo(name = "priv_key_file") var privKeyFileName: String,
    @ColumnInfo(name = "password") var password: String
)

@Dao
interface SshConnectionDataDao {

    @Query("SELECT * FROM connection_table")
    fun getAll(): LiveData<List<SshConnectionData>>

    @Query("SELECT * FROM connection_table WHERE id = :id")
    fun getById(id: Long): LiveData<SshConnectionData>

    @Query("SELECT * FROM connection_table WHERE id = :id")
    fun getByIdSync(id: Long): SshConnectionData

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(sshConnectionData: SshConnectionData): Long

    @Update
    fun update(sshConnectionData: SshConnectionData)

    @Query("DELETE FROM connection_table WHERE id = :id")
    fun delete(id:Long):Int
}

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