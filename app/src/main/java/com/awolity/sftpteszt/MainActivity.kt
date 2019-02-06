package com.awolity.sftpteszt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.awolity.sftpteszt.ssh.ConnectionData
import com.awolity.sftpteszt.ssh.SshTest
import kotlinx.android.synthetic.main.activity_main.*
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.security.Security
import java.util.*

class MainActivity : AppCompatActivity(), RemoteFileAdapter.RemoteFileListener {

    private val sshTest = SshTest()
    private var client: SSHClient? = null
    private lateinit var adapter: RemoteFileAdapter
    private val dirs = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)

        btn_connect.setOnClickListener {
            Log.d(TAG, "Connecting...")
            val connectionData = ConnectionData("192.168.2.76", 22, "user", "asdf")
            sshTest.connect(this, connectionData, object : SshTest.ConnectListener {
                override fun onConnected(client: SSHClient) {
                    this@MainActivity.client = client
                    Log.d(TAG, "...onConnected")
                    dirs.push("/")
                    listDirectory("/")
                }

                override fun onVerifyError(e: Exception) {
                    Log.d(TAG, "...onVerifyError: " + e.localizedMessage)
                }

                override fun onConnectionError(e: Exception) {
                    Log.d(TAG, "...onConnectionError: " + e.localizedMessage)
                    Log.d(TAG, "...cause: " + e.cause)
                }
            })
        }


        setupRv()
    }

    private fun listDirectory(path: String) {
        client.let {
            sshTest.listDirectory(client, path, object : SshTest.ListDirectoryListener {
                override fun onDirectoryListed(remoteFiles: MutableList<RemoteResourceInfo>) {
                    Log.d(TAG, "...onDirectoryListed: \n")
                    AppExecutors.getInstance().mainThread().execute {
                        adapter.updateItems(remoteFiles)
                    }
                }

                override fun onError(e: Exception?) {
                    Log.d(TAG, "...onError: " + e?.localizedMessage)
                    Log.d(TAG, "...cause: " + e?.cause)
                }
            })
        }
    }

    private fun setupRv() {
        adapter = RemoteFileAdapter(
            layoutInflater,
            this as RemoteFileAdapter.RemoteFileListener, true
        )
        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val dividerItemDecoration = DividerItemDecoration(rv_remote.context, linearLayoutManager.orientation)
        rv_remote.addItemDecoration(dividerItemDecoration)
        rv_remote.layoutManager = linearLayoutManager
        rv_remote.adapter = adapter
    }

    override fun onItemClicked(item: RemoteResourceInfo) {
        Log.d(TAG, "onItemClicked - name:" + item.name + " path: " + item.path + " parent: " + item.parent)
        dirs.push(item.path)
        listDirectory(item.path)
    }

    override fun onDeleteClicked(item: RemoteResourceInfo) {
        sshTest.deleteFile(client, item, object : SshTest.DeleteListener {
            override fun onFileDeleted(name: String?) {
                Log.d(TAG, "onFileDeleted - $name")
                listDirectory(dirs.peek())
            }

            override fun onError(e: Exception) {
                Log.d(TAG, "onFileDeleted - ${e.localizedMessage}")
            }
        })
    }

    override fun onLongClicked(item: RemoteResourceInfo) {
        sshTest.downloadFile(client, item, applicationContext.filesDir, object : SshTest.DownloadListener {
            override fun onFileDownloaded(file: File) {
                Log.d(TAG, "onFileDownloaded - " + file.name)
            }

            override fun onError(e: Exception) {
                Log.d(TAG, "onError - " + e.localizedMessage)
            }
        })
    }

    override fun onBackPressed() {
        try {
            val dir = dirs.pop()
            Log.d(TAG, dir)
            listDirectory(dirs.peek())

        } catch (e: NoSuchElementException) {
            super.onBackPressed()
        } catch (e: IOException) {
            Log.e(TAG, "error initializing adapter!", e)
        }

    }

    companion object {
        const val TAG = "MainActivity"
    }
}
