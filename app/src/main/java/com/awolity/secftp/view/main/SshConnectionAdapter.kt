package com.awolity.secftp.view.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.awolity.secftp.R
import com.awolity.secftp.model.SshConnectionData
import com.awolity.secftp.utils.getInitial
import java.util.*

class SshConnectionAdapter(
    private val inflater: LayoutInflater,
    private val sshConnectionListener: SshConnectionListener
) : RecyclerView.Adapter<SshConnectionAdapter.SshConnectionViewHolder>() {

    private val items = ArrayList<SshConnectionData>()

    interface SshConnectionListener {
        fun onItemClicked(item: SshConnectionData)

        fun onLongClicked(item: SshConnectionData, itemView: View)
    }

    fun updateItems(newItems: List<SshConnectionData>?) {
        val oldItems = ArrayList(this.items)
        this.items.clear()
        if (newItems != null) {
            this.items.addAll(newItems)
        }
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return oldItems.size
            }

            override fun getNewListSize(): Int {
                return items.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == if (newItems != null) newItems[newItemPosition] else null
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == newItems!![newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SshConnectionViewHolder {
        val v = inflater.inflate(R.layout.item_connection_data, parent, false)
        return SshConnectionViewHolder(v, sshConnectionListener)
    }

    override fun onBindViewHolder(holder: SshConnectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class SshConnectionViewHolder(itemView: View, private val listener: SshConnectionListener) :
        RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.tv_name)
        private val addressTextView: TextView = itemView.findViewById(R.id.tv_address)
        private val usernameTextView: TextView = itemView.findViewById(R.id.tv_username)
        private val authTypeTextView: TextView = itemView.findViewById(R.id.tv_auth)
        private val clickOverlay: FrameLayout = itemView.findViewById(R.id.fl_click_overlay)
        private val initialImageView: ImageView = itemView.findViewById(R.id.iv_initial)

        fun bind(item: SshConnectionData) {
            clickOverlay.setOnClickListener { listener.onItemClicked(item) }
            clickOverlay.setOnLongClickListener {
                listener.onLongClicked(item, itemView)
                true
            }

            nameTextView.text = item.name
            addressTextView.text =
                itemView.context.getString(R.string.mainact_item_address, item.address, item.port)
            usernameTextView.text =
                itemView.context.getString(R.string.mainact_item_username, item.username)
            authTypeTextView.text =
                if (item.authMethod == 0)
                    itemView.context.getString(R.string.mainact_item_auth_type_pw)
                else
                    itemView.context.getString(R.string.mainact_item_auth_type_pw)

            var firstLetter = ""
            if (item.name.isNotEmpty()) {
                firstLetter = item.name.substring(0, 1)
            }
            initialImageView.setImageDrawable(
                getInitial(firstLetter)
            )
        }
    }
}
