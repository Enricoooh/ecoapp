package com.example.ecoapp;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.ItemFriendRequestBinding;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private final List<User> requests;
    private final OnRequestAction listener;

    public interface OnRequestAction {
        void onAction(User user, String action); // "accept" or "decline"
    }

    public FriendRequestAdapter(List<User> requests, OnRequestAction listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendRequestBinding binding = ItemFriendRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = requests.get(position);
        holder.binding.reqName.setText(user.getNickname() != null ? user.getNickname() : user.getName());
        
        holder.binding.btnAccept.setOnClickListener(v -> listener.onAction(user, "accept"));
        holder.binding.btnDecline.setOnClickListener(v -> listener.onAction(user, "decline"));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemFriendRequestBinding binding;
        public ViewHolder(ItemFriendRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
