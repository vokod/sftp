package com.awolity.secftp.view.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.folderselector.FileChooserDialog
import com.awolity.secftp.*
import com.awolity.settingviews.RadiogroupSetting
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.File

class SettingsActivity : AppCompatActivity(), FileChooserDialog.FileCallback {

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
            FileChooserDialog.Builder(this@SettingsActivity)
                .extensionsFilter(*Constants.extensions)
                .show()
        }
    }

    override fun onFileSelection(dialog: FileChooserDialog, file: File) {
        try {
            file.copyTo(File(filesDir, KNOWN_HOSTS_FILE_NAME), overwrite = true)
            if (!bs_know_hosts_file.checked) bs_know_hosts_file.check()
        } catch (e: Exception) {
            Toast.makeText(this, "Some error occurred", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val KNOWN_HOSTS_FILE_NAME = "known_hosts"

        fun getNewIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}
