package com.awolity.secftp.view.connection

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.awolity.secftp.AppExecutors
import com.awolity.secftp.SecftpApplication
import com.awolity.secftp.model.SshConnectionData
import com.awolity.secftp.model.SshConnectionDatabase
import org.apache.commons.validator.routines.InetAddressValidator
import org.apache.commons.validator.routines.UrlValidator
import java.io.File
import java.util.*

class ConnectionDetailsViewModel(application: Application) : AndroidViewModel(application) {

    var id: Long = 0L
    private var db: SshConnectionDatabase = SshConnectionDatabase.getInstance(application)
    private val _finish = MutableLiveData<Boolean>()
    private val _message = MutableLiveData<String>()
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
    }

    fun getConnection(): LiveData<SshConnectionData> {
        if (id == 0L) {
            throw IllegalStateException("Viewmodel not set up")
        }
        return db.connectionDao().getById(id)
    }

    fun validate(sshConnectionData: SshConnectionData) {
        val schemes = arrayOf("http", "https")
        val isUrl =  Patterns.WEB_URL.matcher(sshConnectionData.address).matches()
        val isInetAddress = InetAddressValidator.getInstance().isValid(sshConnectionData.address)
        if (isUrl || isInetAddress) {
            save(sshConnectionData)
        } else {

            _message.value = "Host address is not valid."
        }
    }

    private fun save(sshConnectionData: SshConnectionData) {
        var result = true

        if (sshConnectionData.authMethod > 0) {
            val privKeyFile = (File(sshConnectionData.privKeyFileName))
            val savedPrivKeyFilename = UUID.randomUUID().toString() + privKeyFile.name
            val pubKeyFile = (File(sshConnectionData.pubKeyFileName))
            val savedPubKeyFilename = UUID.randomUUID().toString() + pubKeyFile.name

            if (saveFile(privKeyFile, savedPrivKeyFilename)) {
                sshConnectionData.privKeyFileName = savedPrivKeyFilename
            } else {
                result = false
                _message.value = "Error while saving private key file"
            }

            if (saveFile(pubKeyFile, savedPubKeyFilename)) {
                sshConnectionData.pubKeyFileName = savedPubKeyFilename
            } else {
                result = false
                _message.value = "Error while saving public key file"
            }
        }
        AppExecutors.getInstance().diskIO().execute {
            db.connectionDao().insert(sshConnectionData)
        }

        if (result) {
            _finish.value = true
        }
    }

    private fun saveFile(file: File, newName: String): Boolean {
        var result = true
        try {
            val filesDir = getApplication<SecftpApplication>().filesDir
            file.copyTo(File(filesDir, newName), overwrite = true)
        } catch (e: Exception) {
            result = false
        }
        return result
    }

    companion object {
        private const val TAG = "ConnectionDetailsViewModel"
    }
}