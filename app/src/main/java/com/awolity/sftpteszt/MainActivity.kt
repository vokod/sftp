package com.awolity.sftpteszt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.awolity.sftpteszt.ssh.ConnectionData
import com.awolity.sftpteszt.ssh.RemoteFile
import com.awolity.sftpteszt.ssh.SshTest
import kotlinx.android.synthetic.main.activity_main.*
import net.schmizz.sshj.SSHClient
import java.lang.Exception
import java.security.Security

class MainActivity : AppCompatActivity() {

    private val sshTest = SshTest()
    private var client: SSHClient? = null

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

        btn_list.setOnClickListener {
            Log.d(TAG, "Listing...")
            client.let {
                sshTest.listDirectory(client, object : SshTest.ListDirectoryListener {
                    override fun onDirectoryListed(files: Array<out RemoteFile>) {
                        Log.d(TAG, "...onDirectoryListed: \n")
                        for (remoteFile in files) {
                            Log.d(
                                TAG, remoteFile.name + if (remoteFile.isDirectory) {
                                    " directory"
                                } else {
                                    " file"
                                }
                            )
                        }
                    }

                    override fun onError(e: Exception?) {
                        Log.d(TAG, "...onError: " + e?.localizedMessage)
                        Log.d(TAG, "...cause: " + e?.cause)
                    }
                })
            }
        }


    }

    companion object {
        const val TAG = "MainActivity"
    }
}
