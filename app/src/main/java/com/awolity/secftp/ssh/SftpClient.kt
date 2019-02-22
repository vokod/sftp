package com.awolity.secftp.ssh

import android.util.Log
import com.awolity.secftp.AppExecutors
import com.awolity.secftp.model.SshConnectionData
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.ConnectionException
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.xfer.FileSystemFile

import java.io.File
import java.io.IOException

class SftpClient {

    private val client: SSHClient = SSHClient(AndroidConfig())

    fun connect(
        hostfile: File, data: SshConnectionData,
        listener: ConnectListener
    ) {
        Log.d(TAG, "connect() called with: hostfile = [$hostfile], data = [$data], listener = [$listener]")
        AppExecutors.getInstance().network().execute {
            try {
                client.loadKnownHosts(hostfile)
            } catch (e: IOException) {
                listener.onVerifyError(e)
            }

            try {
                Log.d(TAG, "Connect:")
                client.connect(data.address, data.port)
                Log.d(TAG, "...connected")
                Log.d(TAG, "Authenticate:")

                if (data.authMethod == 0) { // password
                    client.authPassword(data.username, data.password)
                    Log.d(TAG, "...authenticated with password")
                } else { // certificate
                    val keyProvider = client.loadKeys(data.privKeyFileName)
                    client.authPublickey(data.username, keyProvider)
                    Log.d(TAG, "...authenticated with key")
                }
                client.startSession()

                listener.onConnected()
            } catch (e: ConnectionException) {
                listener.onConnectionError(SshException("SshConnectionData error", e))
            } catch (e: TransportException) {
                listener.onConnectionError(SshException("Transport exception error", e))
                // on TransportException: [HOST_KEY_NOT_VERIFIABLE]
            } catch (e: UserAuthException) {
                listener.onConnectionError(SshException("User authentication error", e))
                // on UserAuthException
            } catch (e: IOException) {
                // on Host unreachable
                // on Network unreachable
                // on ConnectException
                listener.onConnectionError(SshException("IO exception error", e))
            }
        }
    }

    fun listDirectory(path: String, listener: ListDirectoryListener) {
        Log.d(TAG, "listDirectory() called with: sshClient = [$client], listener = [$listener]")
        AppExecutors.getInstance().network().execute {
            if (!client.isConnected) {
                listener.onError(SshException("Client not connected", null))
            } else if (!client.isAuthenticated) {
                listener.onError(SshException("Client not authenticated", null))
            } else {
                try {
                    client.newSFTPClient().use { sftp ->
                        val remoteFiles = sftp.ls(path)
                        listener.onDirectoryListed(remoteFiles)
                    }
                } catch (e: IOException) {
                    listener.onError(SshException("IO exception error", e))
                }

            }
        }
    }

    fun downloadFile(remoteFile: RemoteResourceInfo, inputDir: File, listener: DownloadListener) {
        AppExecutors.getInstance().network().execute(Runnable {
            val inFile = File(inputDir, remoteFile.name)
            if (remoteFile.isDirectory || !remoteFile.isRegularFile) {
                listener.onError(SshException("Remote file is a directory!", null))
                return@Runnable
            }
            if (!client.isConnected) {
                listener.onError(SshException("Client not connected", null))
            } else if (!client.isAuthenticated) {
                listener.onError(SshException("Client not authenticated", null))
            } else {
                try {
                    client.newSFTPClient().use { sftp -> sftp.get(remoteFile.name, FileSystemFile(inFile)) }
                } catch (e: IOException) {
                    listener.onError(SshException("IO exception error", e))
                }

            }
            listener.onFileDownloaded(inFile)
        })
    }

    fun deleteFile(remoteFile: RemoteResourceInfo, listener: DeleteListener) {
        AppExecutors.getInstance().network().execute(Runnable {
            if (!client.isConnected) {
                listener.onError(SshException("Client not connected", null))
                return@Runnable
            } else if (!client.isAuthenticated) {
                listener.onError(SshException("Client not authenticated", null))
                return@Runnable
            }
            if (remoteFile.isDirectory) {
                try {
                    client.newSFTPClient().use { sftp ->
                        sftp.rmdir(remoteFile.name)
                        listener.onFileDeleted(remoteFile.name)
                    }
                } catch (e: IOException) {
                    listener.onError(SshException("IO exception error", e))
                }

            } else {
                try {
                    client.newSFTPClient().use { sftp ->
                        sftp.rm(remoteFile.path)
                        listener.onFileDeleted(remoteFile.name)
                    }
                } catch (e: IOException) {
                    listener.onError(SshException("IO exception error", e))
                }

            }
        })
    }

    fun uploadFile(localFile: File, remotePath: String, listener: UploadListener) {
        AppExecutors.getInstance().network().execute(Runnable {
            if (!client.isConnected) {
                listener.onError(SshException("Client not connected", null))
                return@Runnable
            } else if (!client.isAuthenticated) {
                listener.onError(SshException("Client not authenticated", null))
                return@Runnable
            }
            try {
                client.newSFTPClient().use { sftp ->
                    sftp.put(FileSystemFile(localFile.absolutePath), remotePath + "/" + localFile.name)
                    listener.onFileUploaded(localFile.name)
                }
            } catch (e: IOException) {
                listener.onError(SshException("IO exception error", e))
            }
        })
    }

    fun disconnect(listener: DisconnectListener) {
        AppExecutors.getInstance().network().execute {
            try {
                client.disconnect()
                listener.onDisconnected()
            } catch (e: IOException) {
                listener.onError(SshException("IO error exception", e))
            }
        }
    }

    interface ConnectListener {
        fun onConnected()

        fun onVerifyError(e: Exception)

        fun onConnectionError(e: Exception)
    }

    interface DisconnectListener {
        fun onDisconnected()

        fun onError(e: Exception)
    }

    interface ListDirectoryListener {
        fun onDirectoryListed(remoteFiles: List<RemoteResourceInfo>)

        fun onError(e: Exception)
    }

    interface DownloadListener {
        fun onFileDownloaded(file: File)

        fun onError(e: Exception)
    }

    interface DeleteListener {
        fun onFileDeleted(name: String)

        fun onError(e: Exception)
    }

    interface UploadListener {
        fun onFileUploaded(result: String)

        fun onError(e: Exception)
    }

    companion object {
        private const val TAG = "SftpClient"
    }
}
