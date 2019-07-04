package com.awolity.secftp.ssh

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.awolity.secftp.R
import com.awolity.secftp.utils.AppExecutors
import com.awolity.secftp.model.SshConnectionData
import com.awolity.secftp.utils.MyLog
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.ConnectionException
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.xfer.FileSystemFile
import java.io.File
import java.io.IOException

class SftpClient(val context: Context) {

    private val client: SSHClient = SSHClient(AndroidConfig())

    fun isOnline() = client.isAuthenticated

    fun connectToAnything(data: SshConnectionData, listener: ConnectListener) {
        AppExecutors.networkIO().execute {
            client.addHostKeyVerifier(NullHostKeyVerifier())
            connect(data, listener)
        }
    }

    fun connectToKnownHost(hostFile: File, data: SshConnectionData, listener: ConnectListener) {
        AppExecutors.networkIO().execute {
            try {
                client.loadKnownHosts(hostFile)
            } catch (e: IOException) {
                listener.onVerifyError(e)
            }
            connect(data, listener)
        }
    }

    @WorkerThread
    private fun connect(data: SshConnectionData, listener: ConnectListener) {
        try {
            MyLog.d(TAG, "Connect:")
            client.connect(data.address, data.port)
            MyLog.d(TAG, "...connected")
            MyLog.d(TAG, "Authenticate:")

            if (data.authMethod == 0) { // password
                client.authPassword(data.username, data.password)
                MyLog.d(TAG, "...authenticated with password")
            } else { // certificate
                val keyProvider =
                    client.loadKeys(File(context.filesDir, data.privKeyFileName).absolutePath)
                client.authPublickey(data.username, keyProvider)
                MyLog.d(TAG, "...authenticated with key")
            }
            client.startSession()

            listener.onConnected()
        } catch (e: ConnectionException) {
            listener.onConnectionError(
                SshException(context.getString(R.string.sftpclient_onconnectionerror), e)
            )
        } catch (e: TransportException) {
            listener.onConnectionError(
                SshException(context.getString(R.string.sftpclient_transportexception), e)
            )
            // on TransportException: [HOST_KEY_NOT_VERIFIABLE]
        } catch (e: UserAuthException) {
            listener.onConnectionError(
                SshException(context.getString(R.string.sftpclient_userauthexception), e)
            )
            // on UserAuthException
        } catch (e: IOException) {
            // on Host unreachable
            // on Network unreachable
            // on ConnectException
            listener.onConnectionError(
                SshException(context.getString(R.string.sftpclient_ioexception), e)
            )
        }
    }

    fun listDirectory(path: String, listener: ListDirectoryListener) {
        MyLog.d(TAG, "listDirectory() called with: sshClient = [$client], listener = [$listener]")
        AppExecutors.networkIO().execute {
            if (!checkClientConnectedAndAuthenticated(listener)) {
                return@execute
            }
            try {
                client.newSFTPClient().use { sftp ->
                    val remoteFiles = sftp.ls(path)
                    listener.onDirectoryListed(remoteFiles)
                }
            } catch (e: IOException) {
                listener.onError(
                    SshException(context.getString(R.string.sftpclient_ioexception), e)
                )
            }
        }
    }

    fun downloadFile(remoteFile: RemoteResourceInfo, inputDir: File, listener: DownloadListener) {
        AppExecutors.networkIO().execute {
            if (!checkClientConnectedAndAuthenticated(listener)) {
                return@execute
            }
            if (remoteFile.isDirectory || !remoteFile.isRegularFile) {
                listener.onError(SshException(context.getString(R.string.sftpclient_remotefileisadir), null))
                return@execute
            }
            try {
                val inFile = File(inputDir, remoteFile.name)
                client.newSFTPClient()
                    .use { sftp -> sftp.get(remoteFile.path, FileSystemFile(inFile)) }
                listener.onFileDownloaded(inFile)
            } catch (e: IOException) {
                listener.onError(
                    SshException(context.getString(R.string.sftpclient_ioexception), e)
                )
            }
        }
    }

    fun deleteFile(remoteFile: RemoteResourceInfo, listener: DeleteListener) {
        AppExecutors.networkIO().execute {
            if (!checkClientConnectedAndAuthenticated(listener)) {
                return@execute
            }
            if (remoteFile.isDirectory) {
                try {
                    client.newSFTPClient().use { sftp ->
                        sftp.rmdir(remoteFile.name)
                        listener.onFileDeleted(remoteFile.name)
                    }
                } catch (e: IOException) {
                    listener.onError(
                        SshException(context.getString(R.string.sftpclient_ioexception), e)
                    )
                }
            } else {
                try {
                    client.newSFTPClient().use { sftp ->
                        sftp.rm(remoteFile.path)
                        listener.onFileDeleted(remoteFile.name)
                    }
                } catch (e: IOException) {
                    listener.onError(
                        SshException(context.getString(R.string.sftpclient_ioexception), e)
                    )
                }
            }
        }
    }

    fun uploadFile(localFile: File, remotePath: String, listener: UploadListener) {
        AppExecutors.networkIO().execute {
            if (!checkClientConnectedAndAuthenticated(listener)) {
                return@execute
            }
            try {
                client.newSFTPClient().use { sftp ->
                    sftp.put(
                        FileSystemFile(localFile.absolutePath),
                        remotePath + "/" + localFile.name
                    )
                    listener.onFileUploaded(localFile.name)
                }
            } catch (e: IOException) {
                listener.onError(
                    SshException(context.getString(R.string.sftpclient_ioexception), e)
                )
            }
        }
    }

    fun disconnect(listener: DisconnectListener) {
        AppExecutors.networkIO().execute {
            try {
                client.disconnect()
                listener.onDisconnected()
            } catch (e: IOException) {
                listener.onError(
                    SshException(
                        context.getString(R.string.sftpclient_ioexception),
                        e
                    )
                )
            }
        }
    }

    @WorkerThread
    private fun checkClientConnectedAndAuthenticated(listener: SftpListener): Boolean {
        if (!client.isConnected) {
            listener.onError(
                SshException(
                    context.getString(R.string.sftpclient_client_not_connected),
                    null
                )
            )
            return false
        } else if (!client.isAuthenticated) {
            listener.onError(
                SshException(
                    context.getString(R.string.sftpclient_client_not_authenticated),
                    null
                )
            )
            return false
        }
        return true
    }


    interface SftpListener {
        fun onError(e: Exception)
    }

    interface ConnectListener {
        fun onConnected()

        fun onVerifyError(e: Exception)

        fun onConnectionError(e: Exception)
    }

    interface DisconnectListener : SftpListener {
        fun onDisconnected()
    }

    interface ListDirectoryListener : SftpListener {
        fun onDirectoryListed(remoteFiles: List<RemoteResourceInfo>)
    }

    interface DownloadListener : SftpListener {
        fun onFileDownloaded(file: File)
    }

    interface DeleteListener : SftpListener {
        fun onFileDeleted(name: String)
    }

    interface UploadListener : SftpListener {
        fun onFileUploaded(result: String)
    }

    companion object {
        private const val TAG = "SftpClient"
    }
}
