package com.awolity.secftp.view.sftp

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.awolity.secftp.ANIMATION_DURATION
import com.awolity.secftp.R
import kotlinx.android.synthetic.main.activity_sftp.*
import net.schmizz.sshj.sftp.RemoteResourceInfo
import org.jetbrains.anko.toast

class SftpActivity : AppCompatActivity(), RemoteFileAdapter.RemoteFileListener {

    private lateinit var adapter: RemoteFileAdapter
    private val sftpViewModel: SftpViewModel by lazy { ViewModelProviders.of(this).get(SftpViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sftp)
        setupRv()
        setupWidgets()
        setupObservers()
        if (!sftpViewModel.isOnline()) {
            sftpViewModel.connect(intent.getLongExtra(EXTRA_ID, 0L))
        }
    }

    override fun onItemClicked(item: RemoteResourceInfo) {
        if (item.isDirectory) {
            sftpViewModel.listDirectory(item.path, true)
        }
    }

    override fun onLongClicked(item: RemoteResourceInfo, itemView: View) {
        if (item.isRegularFile) {
            val popup = PopupMenu(this@SftpActivity, itemView)
            popup.menuInflater.inflate(R.menu.menu_sftp_popup, popup.menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_item_download -> {
                        sftpViewModel.download(item)
                        true
                    }
                    R.id.menu_item_delete -> {
                        MaterialDialog(this@SftpActivity).show {
                            title(text = "Delete")
                            message(text = "Do you really want to delete the file ${item.name}?")
                            positiveButton { sftpViewModel.delete(item) }
                            negativeButton { dismiss() }
                        }
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
            popup.show()
        }
    }

    override fun onBackPressed() {
        if (sftpViewModel.backOrPop()) {
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
                    positiveButton(text = "Search")
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
        sftpViewModel.isBusy.observe(this, androidx.lifecycle.Observer {
            if (it) {
                startProgress()
            } else {
                stopProgress()
            }
        })

        sftpViewModel.files.observe(this, Observer {
            adapter.updateItems(it)
        })

        sftpViewModel.actualDir.observe(this, Observer {
            tv_dir.text = it.replace("/", "  /  ")
        })

        sftpViewModel.message.observe(this, Observer {
            toast(it)
        })
    }

    private fun startProgress() {
        pb.scaleY = 0.1f
        pb.visibility = VISIBLE
        pb.animate()
            .scaleY(1f)
            .scaleX(1f)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .duration = ANIMATION_DURATION
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
            .duration = ANIMATION_DURATION
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
