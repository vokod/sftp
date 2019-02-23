package com.awolity.secftp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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