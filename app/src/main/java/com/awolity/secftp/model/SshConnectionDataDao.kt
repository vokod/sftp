package com.awolity.secftp.model

import androidx.lifecycle.LiveData
import androidx.room.*

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(sshConnectionData: SshConnectionData): Long

    @Update
    fun update(sshConnectionData: SshConnectionData)

    @Query("DELETE FROM connection_table")
    fun deleteAll()

    @Query("DELETE FROM connection_table WHERE id = :id")
    fun delete(id:Long):Int
}