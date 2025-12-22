package com.example.ecoapp;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoapp.databinding.ItemBadgeBinding;
import java.util.List;

public class BadgesAdapter extends RecyclerView.Adapter<BadgesAdapter.ViewHolder> {

    private final List<Integer> badgeResIds;
    private final OnBadgeClickListener listener;

    public interface OnBadgeClickListener {
        void onBadgeClick(int badgeResId);
    }

    public BadgesAdapter(List<Integer> badgeResIds, OnBadgeClickListener listener) {
        this.badgeResIds = badgeResIds;
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
        int resId = badgeResIds.get(position);
        holder.binding.badgeImage.setImageResource(resId);
        holder.itemView.setOnClickListener(v -> listener.onBadgeClick(resId));
    }

    @Override
    public int getItemCount() {
        return badgeResIds.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBadgeBinding binding;
        public ViewHolder(ItemBadgeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
