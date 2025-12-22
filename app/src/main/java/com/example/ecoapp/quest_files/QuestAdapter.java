package com.example.ecoapp.quest_files;

import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import com.example.ecoapp.R;

//Per non avere problemi di visibilità
import com.ecoapp.android.auth.models.Quest;
import java.util.List;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.ViewHolder> {

    private final List<Quest> questList;
    private final OnQuestClick listener;

    public interface OnQuestClick {
        void onClick(int questId);
    }

    public QuestAdapter(List<Quest> quests, OnQuestClick listener) {
        this.questList = quests;
        this.listener = listener;
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quest, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Quest q = questList.get(position);
        holder.txtName.setText(q.getName());
        holder.progress.setMax(q.getMaxProgress());
        holder.progress.setProgress(q.getActualProgress());
        holder.txtMaxProgress.setText(String.valueOf(q.getMaxProgress()));
        holder.img.setImageResource(q.getImageResId());

        holder.itemView.setOnClickListener(v -> listener.onClick(q.getId()));
    }

    @Override
    public int getItemCount() {
        return questList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txtName;
        TextView txtMaxProgress;
        ProgressBar progress;

        ViewHolder(View v) {
            super(v);
            img = v.findViewById(R.id.imgQuest);
            txtName = v.findViewById(R.id.txtQuestName);
            progress = v.findViewById(R.id.progressQuest);
            txtMaxProgress = v.findViewById(R.id.txtMaxProgress);
        }
    }
}