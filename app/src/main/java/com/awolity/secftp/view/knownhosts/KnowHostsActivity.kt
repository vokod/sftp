package com.awolity.secftp.view.knownhosts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.fileChooser
import com.awolity.secftp.R
import com.awolity.secftp.utils.importKnownHostsFile

import kotlinx.android.synthetic.main.activity_know_host.*
import org.jetbrains.anko.toast

class KnowHostsActivity : AppCompatActivity(), KnownHostsAdapter.KnownHostListener {

    private lateinit var adapter: KnownHostsAdapter
    private lateinit var vm: KnownHostsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_know_host)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        setupWidgets()
        setupRv()
        setupVm()
    }

    override fun onKnownHostDeleteClicked(item: KnownHost) {
        vm.deleteKnownHost(item)
    }

    private fun setupWidgets() {
        fab_add_known_host.setOnClickListener {
            MaterialDialog(this).show {
                fileChooser { _, file ->
                    try {
                        importKnownHostsFile(this@KnowHostsActivity, file)
                        toast("Known hosts imported")
                    } catch (e: Exception) {
                        Toast.makeText(this@KnowHostsActivity, "Some error occurred", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupRv() {
        adapter = KnownHostsAdapter(layoutInflater, this)
        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rv_known_hosts.layoutManager = linearLayoutManager
        rv_known_hosts.adapter = adapter
    }

    private fun setupVm() {
        vm = ViewModelProviders.of(this).get(KnownHostsViewModel::class.java)
        vm.knownHosts.observe(this, Observer {
            adapter.updateItems(it)
        })
    }

    companion object {
        fun getNewIntent(context: Context): Intent {
            return Intent(context, KnowHostsActivity::class.java)
        }
    }

}
