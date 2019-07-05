package com.awolity.secftp.view.sftp

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.awolity.secftp.R
import com.awolity.secftp.SecftpApplication
import com.awolity.secftp.model.SshConnectionDatabase
import com.awolity.secftp.ssh.SftpClient
import com.awolity.secftp.ssh.SftpClient.ConnectListener
import com.awolity.secftp.utils.*
import com.awolity.yavel.Yavel
import com.awolity.yavel.YavelException
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
    private var _message: MutableLiveData<String> = MutableLiveData()
    private var _errorDialogMessage: MutableLiveData<String> = MutableLiveData()
    private val sftpClient: SftpClient
    private val dirs = ArrayDeque<String>()
    private val context: Context
    private var tempDir: String? = null

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

    var errorDialogMessage: LiveData<String> = _errorDialogMessage
        get() = _errorDialogMessage
        private set

    var sortBy = 0

    init {
        _isBusy.postValue(false)
        _files.value = ArrayList()
        sftpClient = SftpClient(application)
        context = application
    }

    fun isOnline(): Boolean = sftpClient.isOnline()

    fun connect(id: Long) {
        _isBusy.postValue(true)
        AppExecutors.diskIO().execute {
            val connectionData =
                SshConnectionDatabase.getInstance(getApplication<SecftpApplication>())
                    .connectionDao()
                    .getByIdSync(id)
            if (connectionData.authMethod == 0) {
                try {
                    connectionData.password = Yavel.get(YAVEL_KEY_ALIAS).decryptString(connectionData.password)
                } catch (e: YavelException) {
                    _errorDialogMessage.postValue("Password decryption error")
                    return@execute
                }

            }
            if (getOnlyTrustedServers(getApplication())) {
                sftpClient.connectToKnownHost(
                    getKnownHostsFile(getApplication()),
                    connectionData,
                    connectListener
                )
            } else {
                sftpClient.connectToAnything(connectionData, connectListener)
            }
        }
    }

    private val connectListener = object : ConnectListener {
        override fun onConnected() {
            MyLog.d(TAG, "...onConnected")
            listDirectory("/", true)
        }

        override fun onVerifyError(e: Exception) {
            MyLog.d(TAG, "...onVerifyError: " + e.localizedMessage)
            _isBusy.postValue(false)
            _errorDialogMessage.postValue(
                context.getString(
                    R.string.sftpvm_known_host_verification_error
                )
            )
        }

        override fun onConnectionError(e: Exception) {
            MyLog.d(TAG, "...onConnectionError: " + e.localizedMessage)
            MyLog.d(TAG, "...cause: " + e.cause)
            _errorDialogMessage.postValue(
                context.getString(R.string.sftpvm_connection_error, e.message)
            )
            _isBusy.postValue(false)
        }
    }

    fun disconnect() {
        _isBusy.postValue(true)
        sftpClient.disconnect(object : SftpClient.DisconnectListener {

            override fun onDisconnected() {
                MyLog.d(TAG, "...onDisconnected")
                _isBusy.postValue(false)
            }

            override fun onError(e: Exception) {
                MyLog.d(TAG, "...onError: ${e.localizedMessage}")
                _errorDialogMessage.postValue(context.getString(R.string.sftpvm_disconnection_error))
            }
        })
    }

    fun upload(file: File) {
        _isBusy.postValue(true)
        sftpClient.uploadFile(file, _actualDir.value!!, object : SftpClient.UploadListener {
            override fun onFileUploaded(result: String) {
                MyLog.d(TAG, "...onFileUploaded: $result")
                listDirectory(_actualDir.value!!, false)
                _message.postValue(context.getString(R.string.sftpvm_file_uploaded))
            }

            override fun onError(e: Exception) {
                MyLog.d(TAG, "...onError: ${e.localizedMessage}")
                _isBusy.postValue(false)
                _message.postValue(
                    context.getString(
                        R.string.sftpvm_file_upoad_error,
                        e.localizedMessage
                    )
                )
            }
        })
    }

    fun download(item: RemoteResourceInfo) {
        _isBusy.postValue(true)
        sftpClient.downloadFile(item, File(Environment.getExternalStorageDirectory(), "/Download"),
            object : SftpClient.DownloadListener {
                override fun onFileDownloaded(file: File) {
                    MyLog.d(TAG, "onFileDownloaded - " + file.name)
                    _isBusy.postValue(false)
                    _message.postValue(context.getString(R.string.sftpvm_file_downloaded))
                }

                override fun onError(e: Exception) {
                    MyLog.d(TAG, "onError - " + e.localizedMessage)
                    _isBusy.postValue(false)
                    _message.postValue(
                        context.getString(
                            R.string.sftpvm_file_download_error,
                            e.localizedMessage
                        )
                    )
                }
            })
    }

    fun delete(item: RemoteResourceInfo) {
        _isBusy.postValue(true)
        sftpClient.deleteFile(item, object : SftpClient.DeleteListener {
            override fun onFileDeleted(name: String) {
                MyLog.d(TAG, "onFileDeleted - $name")
                listDirectory(_actualDir.value!!, false)
                _message.postValue(context.getString(R.string.sftpvm_file_deleted))
            }

            override fun onError(e: Exception) {
                MyLog.d(TAG, "onFileDeleted - ${e.localizedMessage}")
                _isBusy.postValue(false)
                _message.postValue(
                    context.getString(
                        R.string.sftpvm_file_delete_error,
                        e.localizedMessage
                    )
                )
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
                MyLog.d(TAG, "...onDirectoryListed:")
                if (!getShowHiddenFiles(getApplication())) {
                    _files.postValue(sort(filterHidden(remoteFiles)))
                } else {
                    _files.postValue(sort(remoteFiles))
                }
                _actualDir.postValue(path)
                _isBusy.postValue(false)
            }

            override fun onError(e: Exception) {
                MyLog.d(TAG, "...onError: ${e.localizedMessage}")
                MyLog.d(TAG, "...cause: ${e.cause}")
                _message.postValue(
                    context.getString(
                        R.string.sftpvm_dir_list_error,
                        e.localizedMessage
                    )
                )
            }
        })
    }

    fun sort() {
        _files.postValue(sort(_files.value))
    }

    private fun sort(items: List<RemoteResourceInfo>?): List<RemoteResourceInfo>? {
        return when (sortBy) {
            0 -> {
                (items?.sortedBy { it.name })
            }
            1 -> {
                (items?.sortedBy { it.attributes.size })
            }
            2 -> {
                (items?.sortedBy { it.attributes.mtime })
            }
            else -> {
                (items?.sortedBy { it.name })
            }
        }
    }

    private fun filterHidden(items: List<RemoteResourceInfo>): List<RemoteResourceInfo> {
        return items.filter { !it.name.startsWith('.') }
    }

    fun search(text: String) {
        tempDir = _actualDir.value;
        _actualDir.value = "${actualDir.value} Search: \"$text\""
        isSearchedRightNow = true
        _files.postValue(_files.value?.filter {
            it.name.contains(text, ignoreCase = true)
        })
    }

    fun backOrPop(): Boolean {
        MyLog.d(TAG, "back")
        return try {
            if (!isSearchedRightNow) {
                // not from searching, go back a folder
                dirs.pop()
            }
            if (dirs.peek() == null) {
                // if no previous folder, then we are at root, disconnect
                true
            } else {
                // from previous searching, refresh actualdir
                if(isSearchedRightNow){
                    _actualDir.value = tempDir
                    isSearchedRightNow = false
                }
                listDirectory(dirs.peek(), false)
                false
            }
        } catch (e: NoSuchElementException) {
            true
        } catch (e: IOException) {
            MyLog.e(TAG, "error initializing adapter!", e)
            true
        } catch (e: IllegalStateException) {
            MyLog.e(TAG, "IllegalStateException", e)
            true
        }
    }

    companion object {
        private const val TAG = "SftpViewModel"
    }
}