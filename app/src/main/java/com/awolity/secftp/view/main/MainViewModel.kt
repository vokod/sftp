package com.awolity.secftp.view.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.awolity.secftp.AppExecutors
import com.awolity.secftp.SecftpApplication
import com.awolity.secftp.model.SshConnectionData
import com.awolity.secftp.model.SshConnectionDatabase

class MainViewModel(application: Application) : AndroidViewModel(application) {

    fun getConnections():LiveData<List<SshConnectionData>>{
        return SshConnectionDatabase.getInstance(getApplication<SecftpApplication>()).connectionDao().getAll()
    }

    fun deleteConnection(id:Long){
        AppExecutors.getInstance().diskIO().execute{
            SshConnectionDatabase.getInstance(getApplication()).connectionDao().delete(id)
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}