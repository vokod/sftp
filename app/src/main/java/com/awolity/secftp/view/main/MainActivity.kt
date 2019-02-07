package com.awolity.secftp.view.main

import android.animation.Animator
import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.folderselector.FileChooserDialog
import com.awolity.secftp.*
import com.awolity.secftp.ssh.ConnectionData
import com.awolity.secftp.ssh.SftpOperations
import kotlinx.android.synthetic.main.activity_main.*
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.security.Security
import java.util.*

// TODO: permission request
// TODO: host handling

class MainActivity : AppCompatActivity(), RemoteFileAdapter.RemoteFileListener,
    FileChooserDialog.FileCallback {

    private lateinit var adapter: RemoteFileAdapter
    private val mainViewModel: MainViewModel by lazy { ViewModelProviders.of(this).get(MainViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: move this to application class
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)

        btn_connect.setOnClickListener { mainViewModel.connect() }
        btn_discnnect.setOnClickListener { mainViewModel.disconnect() }
        fab_upload.setOnClickListener {
            FileChooserDialog.Builder(this)
                .extensionsFilter(*Constants.extensions)
                .show()
        }

        setupRv()

        mainViewModel.connectionState.observe(this, androidx.lifecycle.Observer {
            when (it) {
                ConnectionState.DISCONNECTED -> {
                    stopProgress()
                }
                ConnectionState.CONNECTING -> {
                    startProgress()
                }
                ConnectionState.CONNECTED -> {
                    stopProgress()
                }
                ConnectionState.BUSY -> {
                    startProgress()
                }
                ConnectionState.DISCONNECTING -> {
                    startProgress()
                }
                null -> {
                }
            }
        })

        mainViewModel.files.observe(this, androidx.lifecycle.Observer {
            adapter.updateItems(it)
        })

        mainViewModel.actualDir.observe(this, androidx.lifecycle.Observer {
            tv_dir.text = it
        })
    }

    private fun setupRv() {
        adapter = RemoteFileAdapter(
            layoutInflater,
            this as RemoteFileAdapter.RemoteFileListener
        )
        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val dividerItemDecoration = DividerItemDecoration(rv_remote.context, linearLayoutManager.orientation)
        rv_remote.addItemDecoration(dividerItemDecoration)
        rv_remote.layoutManager = linearLayoutManager
        rv_remote.adapter = adapter
    }

    override fun onFileSelection(dialog: FileChooserDialog, file: File) {
        mainViewModel.upload(file)
    }

    override fun onItemClicked(item: RemoteResourceInfo) {
        if (item.isDirectory) {
            mainViewModel.listDirectory(item.path, true)
        }
    }

    override fun onDeleteClicked(item: RemoteResourceInfo) {
        mainViewModel.delete(item)
    }

    override fun onLongClicked(item: RemoteResourceInfo) {
        if (item.isRegularFile) {
            mainViewModel.download(item)
        }
    }

    override fun onBackPressed() {
        if (mainViewModel.backOrPop()) {
            super.onBackPressed()
        }
    }

    private fun startProgress() {
        pb.scaleY = 0.1f
        pb.visibility = VISIBLE
        pb.animate()
            .scaleY(1f)
            .scaleX(1f)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .duration = Constants.ANIMATION_DURATION
    }

    private fun stopProgress() {
        pb.animate()
            .scaleY(0.1f)
            .scaleX(0.1f)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    pb.visibility = INVISIBLE
                }

                override fun onAnimationStart(animation: Animator?) {
                }

                override fun onAnimationCancel(animation: Animator?) {

                }
            })
            .duration = Constants.ANIMATION_DURATION

    }

    companion object {
        const val TAG = "MainActivity"
    }
}
