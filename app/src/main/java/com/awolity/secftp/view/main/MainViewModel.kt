package com.awolity.secftp.view.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.awolity.secftp.AppExecutors
import com.awolity.secftp.ConnectionState
import com.awolity.secftp.ssh.ConnectionData
import com.awolity.secftp.ssh.SftpOperations
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var _connectionState: MutableLiveData<ConnectionState> = MutableLiveData()
    var connectionState: LiveData<ConnectionState> = _connectionState
        get() = _connectionState
    private var _files: MutableLiveData<List<RemoteResourceInfo>> = MutableLiveData()
    var files: LiveData<List<RemoteResourceInfo>> = _files
        get() = _files
    private val dirs = ArrayDeque<String>()

    private var _actualDir: MutableLiveData<String> = MutableLiveData()
    var actualDir: LiveData<String> = _actualDir
        get() = _actualDir
    private var client: SSHClient? = null
    private var hostFile: File

    init {
        _connectionState.postValue(ConnectionState.DISCONNECTED)
        _files.value = ArrayList()
        hostFile = File(application.filesDir, "known_hosts")
    }

    fun connect() {
        _connectionState.postValue(ConnectionState.BUSY)
        val connectionData = ConnectionData("192.168.2.76", 22, "user", "asdf")
        SftpOperations.connect(hostFile, connectionData, object : SftpOperations.ConnectListener {
            override fun onConnected(client: SSHClient) {
                this@MainViewModel.client = client
                Log.d(TAG, "...onConnected")
                listDirectory("/", true)
            }

            override fun onVerifyError(e: Exception) {
                Log.d(MainActivity.TAG, "...onVerifyError: " + e.localizedMessage)
                _connectionState.postValue(ConnectionState.DISCONNECTED)
            }

            override fun onConnectionError(e: Exception) {
                Log.d(MainActivity.TAG, "...onConnectionError: " + e.localizedMessage)
                Log.d(MainActivity.TAG, "...cause: " + e.cause)
                _connectionState.postValue(ConnectionState.DISCONNECTED)
            }
        })
    }

    fun disconnect() {}

    fun upload(file: File) {
        _connectionState.postValue(ConnectionState.BUSY)
        SftpOperations.uploadFile(client, file, _actualDir.value, object : SftpOperations.UploadListener {
            override fun onFileUploaded(result: String) {
                Log.d(TAG, "...onFileUploaded: $result")
                listDirectory(_actualDir.value!!, false)
            }

            override fun onError(e: Exception) {
                Log.d(TAG, "...onError: ${e.localizedMessage}")
                // TODO: valami message-et a mainactivitynek
                _connectionState.postValue(ConnectionState.CONNECTED)
            }
        })
    }

    fun download(item: RemoteResourceInfo) {
        _connectionState.postValue(ConnectionState.BUSY)
        SftpOperations.downloadFile(client, item,
            getApplication<Application>().filesDir,
            object : SftpOperations.DownloadListener {
                override fun onFileDownloaded(file: File) {
                    Log.d(TAG, "onFileDownloaded - " + file.name)
                    _connectionState.postValue(ConnectionState.CONNECTED)
                    // TODO: message
                }

                override fun onError(e: Exception) {
                    Log.d(TAG, "onError - " + e.localizedMessage)
                    _connectionState.postValue(ConnectionState.CONNECTED)
                    //TODO: message
                }
            })
    }

    fun delete(item: RemoteResourceInfo) {
        _connectionState.postValue(ConnectionState.BUSY)
        SftpOperations.deleteFile(client, item, object : SftpOperations.DeleteListener {
            override fun onFileDeleted(name: String?) {
                Log.d(TAG, "onFileDeleted - $name")
                listDirectory(_actualDir.value!!, false)
                // TODO: message
            }

            override fun onError(e: Exception) {
                Log.d(TAG, "onFileDeleted - ${e.localizedMessage}")
                _connectionState.postValue(ConnectionState.CONNECTED)
                // TODO: message
            }
        })
    }

    fun listDirectory(path: String, shallPush: Boolean) {
        if (shallPush) {
            dirs.push(path)
        }
        if (_connectionState.value != ConnectionState.BUSY) {
            _connectionState.postValue(ConnectionState.BUSY)
        }
        client.let {
            SftpOperations.listDirectory(client, path, object : SftpOperations.ListDirectoryListener {
                override fun onDirectoryListed(remoteFiles: MutableList<RemoteResourceInfo>) {
                    Log.d(TAG, "...onDirectoryListed: \n")
                    AppExecutors.getInstance().mainThread().execute {
                        _files.value = remoteFiles
                        _actualDir.value = path
                        _connectionState.postValue(ConnectionState.CONNECTED)
                    }
                }

                override fun onError(e: Exception?) {
                    Log.d(TAG, "...onError: " + e?.localizedMessage)
                    Log.d(TAG, "...cause: " + e?.cause)
                    // TODO: _connectionState?
                }
            })
        }
    }

    fun backOrPop(): Boolean {
        Log.d(TAG, "back")
        return try {
            val dir = dirs.pop()
            Log.d(TAG, dir)
            listDirectory(dirs.peek(), false)
            false
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
        private const val TAG = "MainViewModel"
    }
}