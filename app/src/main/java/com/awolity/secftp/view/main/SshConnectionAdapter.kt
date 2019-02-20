package com.awolity.secftp.view.main

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.awolity.secftp.R
import com.awolity.secftp.model.SshConnectionData
import java.util.*
import android.view.LayoutInflater

class SshConnectionAdapter(private val inflater: LayoutInflater,
    private val sshConnectionListener: SshConnectionListener
) : RecyclerView.Adapter<SshConnectionAdapter.SshConnectionViewHolder>() {

    private val items = ArrayList<SshConnectionData>()

    interface SshConnectionListener {
        fun onItemClicked(item: SshConnectionData)

        fun onDeleteClicked(item: SshConnectionData)

        fun onLongClicked(item: SshConnectionData)
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
        private val deleteImageView: ImageView = itemView.findViewById(R.id.iv_delete_item_incoming)
        private val initialImageView: ImageView = itemView.findViewById(R.id.iv_initial)

        fun bind(item: SshConnectionData) {
            clickOverlay.setOnClickListener { listener.onItemClicked(item) }
            clickOverlay.setOnLongClickListener {
                listener.onLongClicked(item)
                true
            }
            deleteImageView.setOnClickListener { listener.onDeleteClicked(item) }

            nameTextView.text = item.name
            addressTextView.text = "Address: ${item.address}:${item.port}"
            //portTextView.text = "Port: ${item.port.toString()}"
            usernameTextView.text = "Username: ${item.username}"
            authTypeTextView.text = if (item.authMethod == 0) "Auth. type: Password" else "Auth. type: Certificate"

            var firstLetter = ""
            if (!item.name.isEmpty()) {
                firstLetter = item.name.substring(0, 1)
            }
            initialImageView.setImageDrawable(
                getInitial(
                    firstLetter,item.id.toString(), initialImageView.layoutParams.width
                )
            )
        }

        private fun getInitial(firstLetter: String, colorBase: String, widthInPixels: Int): Drawable {
            val generator = ColorGenerator.MATERIAL
            return TextDrawable.builder()
                .beginConfig()
                .width(widthInPixels)  // width in px
                .height(widthInPixels) // height in px
                .endConfig()
                .buildRound(firstLetter, generator.getColor(colorBase))
        }
    }
}
