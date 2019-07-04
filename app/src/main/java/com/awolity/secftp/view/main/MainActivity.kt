package com.awolity.secftp.view.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.awolity.secftp.R
import com.awolity.secftp.utils.getOnlyTrustedServers
import com.awolity.secftp.utils.isHostKnown
import com.awolity.secftp.model.SshConnectionData
import com.awolity.secftp.view.connection.ConnectionDetailsActivity
import com.awolity.secftp.view.settings.SettingsActivity
import com.awolity.secftp.view.sftp.SftpActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SshConnectionAdapter.SshConnectionListener {

    private lateinit var adapter: SshConnectionAdapter
    private lateinit var vm: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupWidgets()
        checkPermission()
        setupRv()
        setupVm()
    }

    override fun onStart() {
        super.onStart()
        checkPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.menu_item_settings ->
                startActivity(SettingsActivity.getNewIntent(this))
        }
        return true
    }

    override fun onItemClicked(item: SshConnectionData) {
        // if trusted servers required, than only start activity if host is known
        if (!getOnlyTrustedServers(this) || isHostKnown(
                this,
                item.address
            )
        ) {
            startActivity(SftpActivity.getNewIntent(this, item.id, item.name))
        } else {
            MaterialDialog(this).show {
                title(text = getString(R.string.mainact_untrusted_dialog_title))
                message(text = getString(R.string.mainact_untrusted_dialog_content))
                positiveButton { dismiss() }
                negativeButton(text = getString(R.string.mainact_untrusted_dialog_action)) {
                    startActivity(SettingsActivity.getNewIntent(this@MainActivity))
                }
            }
        }
    }

    override fun onLongClicked(item: SshConnectionData, itemView: View) {
        // startActivity(ConnectionDetailsActivity.getNewIntent(this, item.id))
        val popup = PopupMenu(this@MainActivity, itemView)
        popup.menuInflater.inflate(R.menu.menu_main_popup, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_item_edit -> {
                    startActivity(ConnectionDetailsActivity.getNewIntent(this, item.id))
                    true
                }
                R.id.menu_item_delete -> {
                    MaterialDialog(this@MainActivity).show {
                        title(text = getString(R.string.mainact_delete_dialog_title))
                        message(text = getString(R.string.mainact_delete_dialog_content, item.name))
                        positiveButton { vm.deleteConnection(item) }
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

    private fun setupWidgets() {
        fab_add_host.setOnClickListener {
            startActivity(
                ConnectionDetailsActivity.getNewIntent(
                    this@MainActivity,
                    0
                )
            )
        }
    }

    private fun checkPermission() {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(
                DialogOnDeniedPermissionListener.Builder
                    .withContext(this)
                    .withTitle(getString(R.string.mainact_permission_dialog_title))
                    .withMessage(getString(R.string.mainact_permission_dialog_content))
                    .withButtonText(android.R.string.ok)
                    .withIcon(R.drawable.ic_sd_storage)
                    .build()
            )
            .check()
    }

    private fun setupRv() {
        adapter =
            SshConnectionAdapter(layoutInflater, this as SshConnectionAdapter.SshConnectionListener)
        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rv_hosts.layoutManager = linearLayoutManager
        rv_hosts.adapter = adapter
    }

    private fun setupVm() {
        vm = ViewModelProviders.of(this).get(MainViewModel::class.java)
        vm.getConnections().observe(this, Observer {
            adapter.updateItems(it)
        })
    }

    companion object {
        fun getNewIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }
}
