package com.awolity.secftp.view.connection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.folderselector.FileChooserDialog
import com.awolity.secftp.Constants
import com.awolity.secftp.R
import com.awolity.secftp.model.SshConnectionData
import com.awolity.settingviews.RadiogroupSetting
import kotlinx.android.synthetic.main.activity_connection.*
import java.io.File

class ConnectionDetailsActivity : AppCompatActivity(), FileChooserDialog.FileCallback {

    private lateinit var vm: ConnectionDetailsViewModel
    private var pubKeyFile: String = ""
    private var privKeyFile: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        vm = ViewModelProviders.of(this).get(ConnectionDetailsViewModel::class.java)
        setupWidgets()
        setupObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_connection, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.menu_item_check ->
                vm.validate(
                    SshConnectionData(
                        0,
                        tiet_title.text.toString(),
                        tiet_address.text.toString(),
                        tiet_username.text.toString(),
                        tiet_port.text.toString().toInt(),
                        rs_auth_type.getSelectedRadioButton(),
                        pubKeyFile,
                        privKeyFile,
                        tiet_password.text.toString()
                    )
                )
        }
        return true
    }

    override fun onFileSelection(dialog: FileChooserDialog, file: File) {
        if (dialog.tag == TAG_PRIV_KEY) {
            privKeyFile = file.absolutePath
        } else if (dialog.tag == TAG_PUB_KEY) {
            pubKeyFile = file.absolutePath
        }
    }

    private fun setupWidgets() {
        rs_auth_type.setListener(object : RadiogroupSetting.RadiogroupSettingListener {
            override fun OnRadioButtonClicked(selected: Int) {
                if (selected == 0) { // password
                    bs_priv_key.isEnabled = false
                    bs_pub_key.isEnabled = false
                    tiet_password.isEnabled = true
                } else { //key
                    bs_priv_key.isEnabled = true
                    bs_pub_key.isEnabled = true
                    tiet_password.isEnabled = false
                }
            }
        })

        bs_pub_key.setOnClickListener {
            FileChooserDialog.Builder(this@ConnectionDetailsActivity)
                .tag(TAG_PUB_KEY)
                .extensionsFilter(*Constants.extensions)
                .show()
        }

        bs_priv_key.setOnClickListener {
            FileChooserDialog.Builder(this@ConnectionDetailsActivity)
                .tag(TAG_PRIV_KEY)
                .extensionsFilter(*Constants.extensions)
                .show()
        }
    }

    private fun setupObservers() {
        setupDataObserver()
        setupMessageObserver()
        setupFinishObserver()
    }

    private fun setupDataObserver() {
        if (intent?.extras!!.containsKey(EXTRA_ID)) {
            if (intent.getLongExtra(EXTRA_ID, 0L) != 0L) {
                vm.id = intent.getLongExtra(EXTRA_ID, 0L)
                vm.getConnection().observe(this, Observer {
                    tiet_title.setText(it.name)
                    tiet_address.setText(it.address)
                    tiet_port.setText(it.port.toString())
                    tiet_username.setText(it.username)
                    tiet_password.setText(it.password)
                    rs_auth_type.setSelectedRadioButton(it.authMethod)
                    if (File(filesDir, it.privKeyFileName).exists()) {
                        bs_priv_key.checked = true
                        privKeyFile = it.privKeyFileName
                    }
                    if (File(filesDir, it.pubKeyFileName).exists()) {
                        bs_pub_key.checked = true
                        pubKeyFile = it.pubKeyFileName
                    }
                })
            }
        }
    }

    private fun setupMessageObserver() {
        vm.message.observe(this, Observer {
            Toast.makeText(this@ConnectionDetailsActivity, it, Toast.LENGTH_LONG).show()
        })
    }

    private fun setupFinishObserver() {
        vm.finish.observe(this, Observer {
            if (it) finish()
        })
    }

    companion object {
        const val EXTRA_ID = "extra id"
        const val TAG_PRIV_KEY = "private key"
        const val TAG_PUB_KEY = "public key"
        fun getNewIntent(context: Context, id: Long): Intent {
            val intent = Intent(context, ConnectionDetailsActivity::class.java)
            intent.putExtra(EXTRA_ID, id)
            return intent
        }
    }
}
