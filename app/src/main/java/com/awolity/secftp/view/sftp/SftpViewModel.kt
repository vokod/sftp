package com.awolity.secftp.view.sftp

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.awolity.secftp.AppExecutors
import com.awolity.secftp.SecftpApplication
import com.awolity.secftp.getKnownHostsFile
import com.awolity.secftp.getOnlyTrustedServers
import com.awolity.secftp.model.SshConnectionDatabase
import com.awolity.secftp.ssh.SftpClient
import com.awolity.secftp.ssh.SftpClient.ConnectListener
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class SftpViewModel(application: Application) : AndroidViewModel(application) {

    private var isSearchedRightNow = false
    private var _files: MutableLiveData<List<RemoteResourceInfo>> = MutableLiveData()
    private var _actualDir: MutableLiveData<String> = MutableLiveData()
    private var _isBusy: MutableLiveData<Boolean> = MutableLiveData()
    private val sftpClient: SftpClient
    private val dirs = ArrayDeque<String>()
    private var _message: MutableLiveData<String> = MutableLiveData()

    var isBusy: LiveData<Boolean> = _isBusy
        get() = _isBusy
        private set

    var files: LiveData<List<RemoteResourceInfo>> = _files
        get() = _files
        private set

    var actualDir: LiveData<String> = _actualDir
        get() = _actualDir
        private set

    var message: LiveData<String> = _message
        get() = _message
        private set

    var sortBy = 0

    init {
        _isBusy.postValue(false)
        _files.value = ArrayList()
        sftpClient = SftpClient(application)
    }

    fun isOnline(): Boolean = sftpClient.isOnline()

    fun connect(id: Long) {
        _isBusy.postValue(true)
        AppExecutors.getInstance().diskIO().execute {
            val connectionData = SshConnectionDatabase.getInstance(getApplication<SecftpApplication>()).connectionDao()
                .getByIdSync(id)
            if (getOnlyTrustedServers(getApplication())) {
                sftpClient.connectToKnownHost(getKnownHostsFile(getApplication()), connectionData, connectListener)
            } else {
                sftpClient.connectToAnything(connectionData, connectListener)
            }
        }
    }

    val connectListener = object : ConnectListener {
        override fun onConnected() {
            Log.d(TAG, "...onConnected")
            listDirectory("/", true)
        }

        override fun onVerifyError(e: Exception) {
            Log.d(SftpActivity.TAG, "...onVerifyError: " + e.localizedMessage)
            _isBusy.postValue(false)
        }

        override fun onConnectionError(e: Exception) {
            Log.d(SftpActivity.TAG, "...onConnectionError: " + e.localizedMessage)
            Log.d(SftpActivity.TAG, "...cause: " + e.cause)
            _isBusy.postValue(false)
        }
    }

    private fun disconnect() {
        _isBusy.postValue(true)
        sftpClient.disconnect(object : SftpClient.DisconnectListener {

            override fun onDisconnected() {
                Log.d(SftpActivity.TAG, "...onDisconnected")
                _isBusy.postValue(false)
            }

            override fun onError(e: Exception) {
                Log.d(SftpActivity.TAG, "...onError: ${e.localizedMessage}")
                _message.postValue("Error while disconnecting: ${e.localizedMessage}")
            }
        })
    }

    fun upload(file: File) {
        _isBusy.postValue(true)
        sftpClient.uploadFile(file, _actualDir.value!!, object : SftpClient.UploadListener {
            override fun onFileUploaded(result: String) {
                Log.d(TAG, "...onFileUploaded: $result")
                listDirectory(_actualDir.value!!, false)
                _message.postValue("File uploaded")
            }

            override fun onError(e: Exception) {
                Log.d(TAG, "...onError: ${e.localizedMessage}")
                _isBusy.postValue(false)
                _message.postValue("File upload error: ${e.localizedMessage}")
            }
        })
    }

    fun download(item: RemoteResourceInfo) {
        _isBusy.postValue(true)
        sftpClient.downloadFile(item, File(Environment.getExternalStorageDirectory(),"/Download"),
            object : SftpClient.DownloadListener {
                override fun onFileDownloaded(file: File) {
                    Log.d(TAG, "onFileDownloaded - " + file.name)
                    _isBusy.postValue(false)
                    _message.postValue("File downloaded")
                }

                override fun onError(e: Exception) {
                    Log.d(TAG, "onError - " + e.localizedMessage)
                    _isBusy.postValue(false)
                    _message.postValue("File download error: ${e.localizedMessage}")
                }
            })
    }

    fun delete(item: RemoteResourceInfo) {
        _isBusy.postValue(true)
        sftpClient.deleteFile(item, object : SftpClient.DeleteListener {
            override fun onFileDeleted(name: String) {
                Log.d(TAG, "onFileDeleted - $name")
                listDirectory(_actualDir.value!!, false)
                _message.postValue("File deleted")
            }

            override fun onError(e: Exception) {
                Log.d(TAG, "onFileDeleted - ${e.localizedMessage}")
                _isBusy.postValue(false)
                _message.postValue("File delete error: ${e.localizedMessage}")
            }
        })
    }

    fun listDirectory(path: String, shallPush: Boolean) {
        if (shallPush) {
            dirs.push(path)
        }
        _isBusy.postValue(true)

        sftpClient.listDirectory(path, object : SftpClient.ListDirectoryListener {
            override fun onDirectoryListed(remoteFiles: List<RemoteResourceInfo>) {
                Log.d(TAG, "...onDirectoryListed:")
                sort(remoteFiles)
                _actualDir.postValue(path)
                _isBusy.postValue(false)
            }

            override fun onError(e: Exception) {
                Log.d(TAG, "...onError: ${e.localizedMessage}")
                Log.d(TAG, "...cause: ${e.cause}")
                _message.postValue("Directory listing error: ${e.localizedMessage}")
            }
        })
    }

    fun sort() {
        sort(_files.value)
    }

    private fun sort(items: List<RemoteResourceInfo>?) {
        when (sortBy) {
            0 -> {
                _files.postValue(items?.sortedBy { it.name })
            }
            1 -> {
                _files.postValue(items?.sortedBy { it.attributes.size })
            }
            2 -> {
                _files.postValue(items?.sortedBy { it.attributes.mtime })
            }
            else -> {
                _files.postValue(items?.sortedBy { it.name })
            }
        }
    }

    fun search(text: String) {
        isSearchedRightNow = true
        _files.postValue(_files.value?.filter {
            it.name.contains(text, ignoreCase = true)
        })
    }

    fun backOrPop(): Boolean {
        Log.d(TAG, "back")
        return try {
            if (!isSearchedRightNow) {
                dirs.pop()
            }
            if (dirs.peek() == null) {
                disconnect()
                true
            } else {
                listDirectory(dirs.peek(), false)
                false
            }
        } catch (e: NoSuchElementException) {
            true
        } catch (e: IOException) {
            Log.e(TAG, "error initializing adapter!", e)
            true
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException", e)
            true
        }
    }

    companion object {
        private const val TAG = "SftpViewModel"
    }
}