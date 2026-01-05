package com.example.ecoapp;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ecoapp.android.auth.models.Badge;
import com.example.ecoapp.databinding.ItemBadgeBinding;
import java.util.List;

public class BadgesAdapter extends RecyclerView.Adapter<BadgesAdapter.ViewHolder> {

    private final List<Badge> badges;
    private final OnBadgeClickListener listener;

    public interface OnBadgeClickListener {
        void onBadgeClick(Badge badge);
    }

    public BadgesAdapter(List<Badge> badges, OnBadgeClickListener listener) {
        this.badges = badges;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBadgeBinding binding = ItemBadgeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Badge badge = badges.get(position);
        holder.binding.badgeImage.setImageResource(badge.getImageResId());
        holder.binding.badgeName.setText(badge.getName());
        
        holder.itemView.setOnClickListener(v -> listener.onBadgeClick(badge));
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBadgeBinding binding;
        public ViewHolder(ItemBadgeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
