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

    public FriendsAdapter(List<User> friends) {
        this.friends = friends;
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
        holder.binding.friendName.setText(friend.getName());
        // Se il nickname esiste, mostralo tra parentesi o sotto
        if (friend.getNickname() != null) {
            holder.binding.friendName.setText(friend.getName() + " (@" + friend.getNickname() + ")");
        }
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
