package com.awolity.secftp.view.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.awolity.secftp.*
import com.awolity.secftp.utils.*
import com.awolity.secftp.view.knownhosts.KnowHostsActivity
import com.awolity.settingviews.ButtonSetting
import com.awolity.settingviews.RadiogroupSetting
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.toast

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setupWidgets()
    }

    private fun setupWidgets() {
        rs_trusted_server.setSelectedRadioButton(if (getOnlyTrustedServers(this)) 1 else 0)
        rs_trusted_server.setListener(object : RadiogroupSetting.RadiogroupSettingListener {
            override fun OnRadioButtonClicked(selected: Int) {
                setOnlyTrustedServers(this@SettingsActivity, selected > 0)
                if (selected > 0) {
                    bs_known_hosts_file.isEnabled = true
                    bs_edit_known_hosts.isEnabled = true
                } else {
                    bs_known_hosts_file.isEnabled = false
                    bs_edit_known_hosts.isEnabled = false
                }
            }
        })

        val onlyTrustedServers = getOnlyTrustedServers(this)
        bs_known_hosts_file.isEnabled = onlyTrustedServers
        bs_edit_known_hosts.isEnabled = onlyTrustedServers

        val bsKnow =
            findViewById<ButtonSetting>(R.id.bs_known_hosts_file) //valamiert nullnak l'tja a koltin android extension altal letrehozott referenciakat, de csak a materialdialog lambdajaban
        bsKnow.checked = knownHostsFileExist(this)
        bsKnow.setOnClickListener {
            MaterialDialog(this).show {
                fileChooser { _, file ->
                    try {
                        importKnownHostsFile(this@SettingsActivity, file)
                        if (!bsKnow.checked) bsKnow.check()
                        else toast(getString(R.string.settingsact_hosts_imported))
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.settingsact_hosts_import_error), Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            }
        }

        bs_edit_known_hosts.setOnClickListener {
            startActivity(KnowHostsActivity.getNewIntent(this))
        }

        ss_show_hidden_files.checked = getShowHiddenFiles(this)
        ss_show_hidden_files.setOnCheckedChangedListener(CompoundButton.OnCheckedChangeListener {
                _, isChecked ->
            setShowHiddenFiles(this@SettingsActivity, isChecked)
        })
    }

    companion object {

        fun getNewIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}
