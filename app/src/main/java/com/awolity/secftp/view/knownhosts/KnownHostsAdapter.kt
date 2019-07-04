package com.awolity.secftp.view.knownhosts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.awolity.secftp.R
import com.awolity.secftp.utils.getInitial
import java.util.*

class KnownHostsAdapter(
    private val inflater: LayoutInflater, private val listener: (KnownHost) -> Unit
) : RecyclerView.Adapter<KnownHostsAdapter.KnownHostViewHolder>() {

    private val items = ArrayList<KnownHost>()

    fun updateItems(newItems: List<KnownHost>?) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KnownHostViewHolder {
        val v = inflater.inflate(R.layout.item_known_host, parent, false)
        return KnownHostViewHolder(v, listener)
    }

    override fun onBindViewHolder(holder: KnownHostViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class KnownHostViewHolder(itemView: View, private val listener: (KnownHost) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val addressTextView: TextView = itemView.findViewById(R.id.tv_host_address)
        private val metadataTextView: TextView = itemView.findViewById(R.id.tv_key_type)
        private val clickOverlay: FrameLayout = itemView.findViewById(R.id.fl_click_overlay)
        private val initialImageView: ImageView = itemView.findViewById(R.id.iv_initial)

        fun bind(item: KnownHost) {
            clickOverlay.setOnLongClickListener {
                listener(item)
                true
            }

            addressTextView.text = item.address
            metadataTextView.text = "type: ${item.type}"
            val firstLetter = item.address.substring(0, 1)
            initialImageView.setImageDrawable(
                getInitial(firstLetter)
            )
        }
    }
}
