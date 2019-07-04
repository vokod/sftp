package com.awolity.secftp.view.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.awolity.secftp.SecftpApplication
import com.awolity.secftp.model.SshConnectionData
import com.awolity.secftp.model.SshConnectionDatabase
import com.awolity.secftp.utils.AppExecutors
import com.awolity.secftp.utils.deleteFileIfExist

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context

    init {
        context = application
    }

    fun getConnections():LiveData<List<SshConnectionData>>{
        return SshConnectionDatabase.getInstance(getApplication<SecftpApplication>()).connectionDao().getAll()
    }

    fun deleteConnection(sshConnectionData: SshConnectionData) {
        AppExecutors.diskIO().execute{
            SshConnectionDatabase.getInstance(getApplication()).connectionDao().delete(sshConnectionData.id)
            deleteFileIfExist(context, sshConnectionData.privKeyFileName)
        }
    }
}