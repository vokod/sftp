package com.awolity.secftp.view.knownhosts

import android.app.Application
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.awolity.secftp.utils.AppExecutors
import com.awolity.secftp.utils.KNOWN_HOSTS_FILE_NAME
import com.awolity.secftp.utils.readKnownHostsFile
import com.awolity.secftp.utils.writeKnownHostFile
import java.io.File

class KnownHostsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context

    private var _knownHosts: MutableLiveData<MutableList<KnownHost>> = MutableLiveData()

    var knownHosts: LiveData<MutableList<KnownHost>> = _knownHosts
        get() = _knownHosts
        private set

    init {
        context = application
        AppExecutors.getInstance().diskIO().execute {
           loadKnownHostsFile()
        }
    }

    @WorkerThread
    fun loadKnownHostsFile(){
        val knownHostListAsString = readKnownHostsFile(File(context.filesDir, KNOWN_HOSTS_FILE_NAME))
        val result = mutableListOf<KnownHost>()
        knownHostListAsString.forEach {
            val parts = it.split(" ")
            result.add(KnownHost(parts[0], parts[1], parts[2]))
        }
        _knownHosts.postValue(result)
    }

    fun deleteKnownHost(itemToDelete: KnownHost) {
        AppExecutors.getInstance().diskIO().execute {
            _knownHosts.value?.remove(itemToDelete)
            writeKnownHostFile(context, _knownHosts.value as List<KnownHost>)
            loadKnownHostsFile()
        }
    }

    companion object {
        const val TAG = "KnownHostsViewModel"
    }
}