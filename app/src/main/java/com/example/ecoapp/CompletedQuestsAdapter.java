package com.example.ecoapp;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ecoapp.android.auth.models.Quest;
import com.example.ecoapp.databinding.ItemQuestCompletedBinding;
import java.util.List;

public class CompletedQuestsAdapter extends RecyclerView.Adapter<CompletedQuestsAdapter.ViewHolder> {

    private final List<Quest> quests;
    private final OnQuestClickListener listener;

    public interface OnQuestClickListener {
        void onQuestClick(Quest quest);
    }

    public CompletedQuestsAdapter(List<Quest> quests, OnQuestClickListener listener) {
        this.quests = quests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemQuestCompletedBinding binding = ItemQuestCompletedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Quest quest = quests.get(position);
        holder.binding.questName.setText(quest.getName());
        holder.binding.questPoints.setText("+" + (quest.getId() * 10 + 20) + " pt");
        
        holder.itemView.setOnClickListener(v -> listener.onQuestClick(quest));
    }

    @Override
    public int getItemCount() {
        return quests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemQuestCompletedBinding binding;
        public ViewHolder(ItemQuestCompletedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
