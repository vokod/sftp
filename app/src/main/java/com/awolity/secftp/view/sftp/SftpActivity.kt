package com.awolity.secftp.view.sftp

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.folderselector.FileChooserDialog
import com.awolity.secftp.ConnectionState
import com.awolity.secftp.Constants
import com.awolity.secftp.R
import kotlinx.android.synthetic.main.activity_sftp.*
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.File

class SftpActivity : AppCompatActivity(), RemoteFileAdapter.RemoteFileListener,
    FileChooserDialog.FileCallback {

    private lateinit var adapter: RemoteFileAdapter
    private val sftpViewModel: SftpViewModel by lazy { ViewModelProviders.of(this).get(SftpViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sftp)

        setupRv()
        setupWidgets()
        setupObservers()

        sftpViewModel.connect(intent.getLongExtra(EXTRA_ID, 0L))
    }

    private fun setupWidgets(){
        btn_discnnect.setOnClickListener { sftpViewModel.disconnect() }
        fab_upload.setOnClickListener {
            FileChooserDialog.Builder(this)
                .extensionsFilter(*Constants.extensions)
                .show()
        }
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

    private fun setupObservers(){
        sftpViewModel.connectionState.observe(this, androidx.lifecycle.Observer {
            when (it) {
                ConnectionState.DISCONNECTED -> {
                    stopProgress()
                }
                ConnectionState.CONNECTED -> {
                    stopProgress()
                }
                ConnectionState.BUSY -> {
                    startProgress()
                }
                null -> {
                }
            }
        })

        sftpViewModel.files.observe(this, androidx.lifecycle.Observer {
            adapter.updateItems(it)
        })

        sftpViewModel.actualDir.observe(this, androidx.lifecycle.Observer {
            tv_dir.text = it
        })
    }


    override fun onFileSelection(dialog: FileChooserDialog, file: File) {
        sftpViewModel.upload(file)
    }

    override fun onItemClicked(item: RemoteResourceInfo) {
        if (item.isDirectory) {
            sftpViewModel.listDirectory(item.path, true)
        }
    }

    override fun onDeleteClicked(item: RemoteResourceInfo) {
        sftpViewModel.delete(item)
    }

    override fun onLongClicked(item: RemoteResourceInfo) {
        if (item.isRegularFile) {
            sftpViewModel.download(item)
        }
    }

    override fun onBackPressed() {
        if (sftpViewModel.backOrPop()) {
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
        const val TAG = "SftpActivity"
        private const val EXTRA_ID = "id"

        fun getNewIntent(context: Context, id: Long): Intent {
            val intent = Intent(context, SftpActivity::class.java)
            intent.putExtra(EXTRA_ID, id)
            return intent
        }
    }
}
