package com.awolity.secftp.view.sftp

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.awolity.secftp.ConnectionState
import com.awolity.secftp.Constants
import com.awolity.secftp.R
import kotlinx.android.synthetic.main.activity_sftp.*
import net.schmizz.sshj.sftp.RemoteResourceInfo

class SftpActivity : AppCompatActivity(), RemoteFileAdapter.RemoteFileListener {

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
            sftpViewModel.disconnect()
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sftp, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.menu_item_sort -> {
                MaterialDialog(this@SftpActivity).show {
                    listItemsSingleChoice(
                        R.array.sortby,
                        initialSelection = sftpViewModel.sortBy
                    ) { _, index, _ ->
                        sftpViewModel.sortBy = index
                        sftpViewModel.sort()
                    }
                }
            }
            R.id.menu_item_search -> {
                MaterialDialog(this).show {
                    input { _, text ->
                        sftpViewModel.search(text.toString())
                    }
                    positiveButton(R.string.search)
                }
            }
        }
        return true
    }

    private fun setupWidgets() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
        val title = intent.getStringExtra(EXTRA_NAME)
        supportActionBar?.title = title
        fab_upload.setOnClickListener {
            MaterialDialog(this).show {
                fileChooser { _, file ->
                    sftpViewModel.upload(file)
                }
            }
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

    private fun setupObservers() {
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
            tv_dir.text = it.replace("/","  /  ")
        })
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
        private const val EXTRA_NAME = "name"

        fun getNewIntent(context: Context, id: Long, name: String): Intent {
            val intent = Intent(context, SftpActivity::class.java)
            intent.putExtra(EXTRA_ID, id)
            intent.putExtra(EXTRA_NAME, name)
            return intent
        }
    }
}
