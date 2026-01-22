package com.example.ecoapp;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.ItemFriendBinding;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private final List<User> friends;
    private final OnFriendInteractionListener listener;

    public interface OnFriendInteractionListener {
        void onFriendClick(User friend);
        void onRemove(User friend);
    }

    public FriendsAdapter(List<User> friends, OnFriendInteractionListener listener) {
        this.friends = friends;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendBinding binding = ItemFriendBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User friend = friends.get(position);
        
        String displayName = friend.getName();
        if (friend.getNickname() != null) {
            displayName += " (@" + friend.getNickname() + ")";
        }
        holder.binding.friendName.setText(displayName);

        // Click sull'intera riga per vedere il profilo
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFriendClick(friend);
            }
        });

        // Click sul tasto rimuovi
        holder.binding.removeFriendButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(friend);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemFriendBinding binding;
        public ViewHolder(ItemFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
