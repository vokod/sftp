package com.awolity.secftp.view.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.awolity.secftp.*
import com.awolity.settingviews.RadiogroupSetting
import kotlinx.android.synthetic.main.activity_connection.*
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.File

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
            }
        })

        rs_default_auth_method.setSelectedRadioButton(if (getDefaultAuthenticationMethod(this)) 1 else 0)
        rs_default_auth_method.setListener(object : RadiogroupSetting.RadiogroupSettingListener {
            override fun OnRadioButtonClicked(selected: Int) {
                setDefaultAuthenticationMethod(this@SettingsActivity, selected > 0)
            }
        })

        bs_know_hosts_file.checked = File(filesDir, KNOWN_HOSTS_FILE_NAME).exists()
        bs_know_hosts_file.setOnClickListener {
            bs_pub_key.setOnClickListener {
                MaterialDialog(this).show {
                    fileChooser { _, file ->
                        try {
                            file.copyTo(File(filesDir, KNOWN_HOSTS_FILE_NAME), overwrite = true)
                            if (!bs_know_hosts_file.checked) bs_know_hosts_file.check()
                        } catch (e: Exception) {
                            Toast.makeText(this@SettingsActivity, "Some error occurred", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KNOWN_HOSTS_FILE_NAME = "known_hosts"

        fun getNewIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}
