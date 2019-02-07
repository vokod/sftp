package com.awolity.secftp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import net.schmizz.sshj.sftp.RemoteResourceInfo;

import java.util.ArrayList;
import java.util.List;

public class RemoteFileAdapter extends RecyclerView.Adapter<RemoteFileAdapter.RemoteFileItemViewHolder> {

    public interface RemoteFileListener {
        void onItemClicked(@NonNull RemoteResourceInfo item);

        void onDeleteClicked(@NonNull RemoteResourceInfo item);

        void onLongClicked(@NonNull RemoteResourceInfo item);
    }

    private final LayoutInflater inflater;
    private final RemoteFileListener remoteFileListener;
    private List<RemoteResourceInfo> items = new ArrayList<>();

    public RemoteFileAdapter(LayoutInflater inflater,
                             RemoteFileListener remoteFileListener) {
        this.inflater = inflater;
        this.remoteFileListener = remoteFileListener;
    }


    public void remove(RemoteResourceInfo itemToDelete) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == itemToDelete) {
                //TODO
                break;
            }
        }
    }

    public void updateItems(final List<RemoteResourceInfo> newItems) {
        final List<RemoteResourceInfo> oldItems = new ArrayList<>(this.items);
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldItems.size();
            }

            @Override
            public int getNewListSize() {
                return items.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldItems.get(oldItemPosition).equals(newItems != null ? newItems.get(newItemPosition) : null);
            }

            @SuppressWarnings("ConstantConditions")
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return oldItems.get(oldItemPosition).equals(newItems.get(newItemPosition));
            }
        }).dispatchUpdatesTo(this);
    }

    @Override
    public RemoteFileAdapter.RemoteFileItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_incoming, parent, false);

        return new RemoteFileItemViewHolder(v, remoteFileListener);
    }

    @Override
    public void onBindViewHolder(RemoteFileAdapter.RemoteFileItemViewHolder holder, int position) {
        RemoteFileItemViewHolder vh = (RemoteFileItemViewHolder) holder;
        vh.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    static class RemoteFileItemViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final ImageView initialImageView;
        private final ImageView deleteImageView;
        private final LinearLayout itemLinearLayout;
        private final RemoteFileListener listener;

        RemoteFileItemViewHolder(View itemView, final RemoteFileListener listener) {
            super(itemView);
            this.listener = listener;
            titleTextView = itemView.findViewById(R.id.tv_title_item_incoming);
            descriptionTextView = itemView.findViewById(R.id.tv_description_item_incoming);
            deleteImageView = itemView.findViewById(R.id.iv_delete_item_incoming);
            initialImageView = itemView.findViewById(R.id.iv_initial_item_incoming);
            itemLinearLayout = itemView.findViewById(R.id.ll_item_incoming);
        }

        void bind(final RemoteResourceInfo item) {

            itemLinearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClicked(item);
                }
            });

            itemLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    listener.onDeleteClicked(item);
                    return true;
                }
            });

            deleteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onDeleteClicked(item);
                }
            });

            titleTextView.setText(item.getName());
            descriptionTextView.setText("Attribútumok: " + item.getAttributes());

            if(item.isRegularFile()) {
                ColorGenerator generator = ColorGenerator.MATERIAL;
                String firstLetter = item.getName().substring(0, 1);
                TextDrawable drawable = TextDrawable.builder()
                        .buildRound(firstLetter, generator.getColor(item.getName()));
                initialImageView.setImageDrawable(drawable);
            } else if(item.isDirectory()){
                initialImageView.setImageDrawable(initialImageView.getContext().getDrawable(R.drawable.ic_folder));
            }
        }
    }
}
