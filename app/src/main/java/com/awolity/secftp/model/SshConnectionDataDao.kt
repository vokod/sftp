package com.awolity.secftp.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SshConnectionDataDao {

    @Query("SELECT * FROM connection_table")
    fun getAll(): LiveData<List<SshConnectionData>>

    @Query("SELECT * FROM connection_table WHERE id = :id")
    fun getById(id: Long): LiveData<SshConnectionData>

    @Query("SELECT * FROM connection_table WHERE id = :id")
    fun getByIdSync(id: Long): SshConnectionData

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(sshConnectionData: List<SshConnectionData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sshConnectionData: SshConnectionData): Long

    @Query("DELETE FROM connection_table")
    fun deleteAll()
}