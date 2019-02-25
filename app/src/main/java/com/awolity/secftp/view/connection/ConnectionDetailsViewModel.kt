package com.awolity.secftp.view.connection

import android.app.Application
import android.content.Context
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.awolity.secftp.model.SshConnectionData
import com.awolity.secftp.model.SshConnectionDatabase
import com.awolity.secftp.utils.AppExecutors
import com.awolity.secftp.utils.YAVEL_KEY_ALIAS
import com.awolity.secftp.utils.deleteFileIfExist
import com.awolity.secftp.utils.saveFile
import com.awolity.yavel.Yavel
import org.apache.commons.validator.routines.InetAddressValidator
import java.io.File
import java.util.*

class ConnectionDetailsViewModel(application: Application) : AndroidViewModel(application) {

    var id: Long = 0L
    private var db: SshConnectionDatabase = SshConnectionDatabase.getInstance(application)
    private val _finish = MutableLiveData<Boolean>()
    private val _message = MutableLiveData<String>()
    private var context: Context
    val finish: LiveData<Boolean>
        get() {
            return _finish
        }
    val message: LiveData<String>
        get() {
            return _message
        }

    init {
        _finish.value = false
        _message.value = ""
        context = application
    }

    fun getConnection(): LiveData<SshConnectionData> {
        if (id == 0L) {
            throw IllegalStateException("Viewmodel not set up")
        }
        return db.connectionDao().getById(id)
    }

    fun validate(sshConnectionData: SshConnectionData): Boolean {
        val isUrl = Patterns.WEB_URL.matcher(sshConnectionData.address).matches()
        val isInetAddress = InetAddressValidator.getInstance().isValid(sshConnectionData.address)
        if (!(isUrl || isInetAddress)) {
            _message.value = "Host address is not valid."
            return false
        }

        if (sshConnectionData.authMethod == 0) { //password auth
            if (sshConnectionData.password.isEmpty()) {
                _message.value = "Password not valid."
                return false
            }
        } else { // key auth
            if (sshConnectionData.privKeyFileName.isEmpty()) {
                _message.value = "Import private key file."
                return false
            }
        }
        return true
    }

    fun importPrivKeyFile(file: File, oldFileName: String, listener: (File) -> Unit) {
        AppExecutors.getInstance().diskIO().execute {
            deleteFileIfExist(context, oldFileName)
            val savedFileName = keyFilePrefix + file.name
            val savedFile = saveFile(context, file, savedFileName)
            if (savedFile != null) {
                AppExecutors.getInstance().mainThread().execute { listener(savedFile) }
            } else {
                _message.postValue("Error while saving private key file")
            }
        }
    }

    fun save(sshConnectionData: SshConnectionData) {
        AppExecutors.getInstance().diskIO().execute {
            if (sshConnectionData.authMethod == 0) {
                sshConnectionData.password = Yavel.get(YAVEL_KEY_ALIAS).encryptString(sshConnectionData.password)
            }
            if (db.connectionDao().insert(sshConnectionData) == -1L) {
                db.connectionDao().update(sshConnectionData)
            }
            _finish.postValue(true)
        }
    }

    companion object {
        private const val TAG = "ConnectionDetailsViewModel"
        private var keyFilePrefix = UUID.randomUUID().toString() + "_"
    }
}