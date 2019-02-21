package com.awolity.secftp.view.sftp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.awolity.secftp.R
import com.awolity.secftp.getInitial
import com.awolity.secftp.humanReadableByteCount
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.util.*

class RemoteFileAdapter(
    private val inflater: LayoutInflater,
    private val remoteFileListener: RemoteFileListener
) : RecyclerView.Adapter<RemoteFileAdapter.RemoteFileItemViewHolder>() {
    private val items = ArrayList<RemoteResourceInfo>()

    interface RemoteFileListener {
        fun onItemClicked(item: RemoteResourceInfo)

        fun onDeleteClicked(item: RemoteResourceInfo)

        fun onLongClicked(item: RemoteResourceInfo)
    }

    fun updateItems(newItems: List<RemoteResourceInfo>?) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteFileAdapter.RemoteFileItemViewHolder {
        val v = inflater.inflate(R.layout.item_incoming, parent, false)
        return RemoteFileItemViewHolder(v, remoteFileListener)
    }

    override fun onBindViewHolder(holder: RemoteFileAdapter.RemoteFileItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }


    class RemoteFileItemViewHolder(itemView: View, private val listener: RemoteFileListener) :
        RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.tv_title_item_incoming)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tv_description_item_incoming)
        private val initialImageView: ImageView = itemView.findViewById(R.id.iv_initial)
        private val clickOverlay: FrameLayout = itemView.findViewById(R.id.fl_click_overlay)

        fun bind(item: RemoteResourceInfo) {
            clickOverlay.setOnClickListener { listener.onItemClicked(item) }
            clickOverlay.setOnLongClickListener {
                listener.onLongClicked(item)
                true
            }
            titleTextView.text = item.name

            when {
                item.isDirectory -> {
                    initialImageView.setImageDrawable(initialImageView.context.getDrawable(R.drawable.ic_folder))
                    descriptionTextView.text = "Folder"
                }
                else -> {
                    initialImageView.setImageDrawable(getInitial(item.name))
                    descriptionTextView.text =
                        humanReadableByteCount(item.attributes.size, false) + item.attributes.mtime

                }
            }
        }
    }
}
