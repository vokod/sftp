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
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.awolity.secftp.R
import com.awolity.secftp.model.SshConnectionData
import com.awolity.secftp.utils.MyLog
import com.awolity.settingviews.ButtonSetting
import com.awolity.settingviews.RadiogroupSetting
import kotlinx.android.synthetic.main.activity_connection.*
import java.io.File

class ConnectionDetailsActivity : AppCompatActivity() {

    private lateinit var vm: ConnectionDetailsViewModel
    private var privKeyFile: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        vm = ViewModelProviders.of(this).get(ConnectionDetailsViewModel::class.java)
        setupWidgets()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        MyLog.d(TAG, "onResume")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_connection, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.menu_item_check -> {
                val sshConnectionData = SshConnectionData(
                    intent.getLongExtra(EXTRA_ID, 0),
                    tiet_title.text.toString(),
                    tiet_address.text.toString(),
                    tiet_username.text.toString(),
                    tiet_port.text.toString().toInt(),
                    rs_auth_type.getSelectedRadioButton(),
                    privKeyFile,
                    tiet_password.text.toString()
                )
                if (vm.validate(sshConnectionData)) {
                    vm.save(sshConnectionData)
                }
            }
        }
        return true
    }

    private fun setupWidgets() {
        rs_auth_type.setListener(object : RadiogroupSetting.RadiogroupSettingListener {
            override fun OnRadioButtonClicked(selected: Int) {
                if (selected == 0) { // password
                    bs_priv_key.isEnabled = false
                    tiet_password.isEnabled = true
                } else { //key
                    bs_priv_key.isEnabled = true
                    tiet_password.isEnabled = false
                }
            }
        })

        val bsPrivateKey = findViewById<ButtonSetting>(R.id.bs_priv_key)
        bsPrivateKey.setOnClickListener {
            MaterialDialog(this).show {
                bsPrivateKey.checked = false
                fileChooser { _, file ->
                    vm.importPrivKeyFile(file, privKeyFile) {
                        privKeyFile = it.name
                        bsPrivateKey.check()
                    }
                }
            }
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
                    if (it.authMethod == 0) {
                        bs_priv_key.isEnabled = false
                        tiet_password.isEnabled = true
                    } else {
                        bs_priv_key.isEnabled = true
                        tiet_password.isEnabled = false
                    }
                    when {
                        it.privKeyFileName.isEmpty() -> bs_priv_key.checked = false
                        File(filesDir, it.privKeyFileName).exists() -> {
                            bs_priv_key.checked = true
                            privKeyFile = it.privKeyFileName
                        }
                        else -> bs_priv_key.checked = false
                    }
                })
            }
        }
    }

    private fun setupMessageObserver() {
        vm.message.observe(this, Observer {
            if (it.isNotEmpty()) Toast.makeText(this@ConnectionDetailsActivity, it,
                Toast.LENGTH_LONG).show()
        })
    }

    private fun setupFinishObserver() {
        vm.finish.observe(this, Observer {
            if (it) finish()
        })
    }

    companion object {
        const val EXTRA_ID = "extra id"
        const val TAG = "ConnectionDetailsActivity"
        fun getNewIntent(context: Context, id: Long): Intent {
            val intent = Intent(context, ConnectionDetailsActivity::class.java)
            intent.putExtra(EXTRA_ID, id)
            return intent
        }
    }
}
