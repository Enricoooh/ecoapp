package com.example.ecoapp.quest_files;

import android.view.*;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import com.example.ecoapp.R;
import com.ecoapp.android.auth.models.LocalQuest;

import java.util.List;

public class CompletedQuestAdapter extends RecyclerView.Adapter<CompletedQuestAdapter.ViewHolder> {

    private final List<LocalQuest> questList;
    private final OnQuestClick listener;

    public interface OnQuestClick {
        void onClick(int questId);
    }

    public CompletedQuestAdapter(List<LocalQuest> quests, OnQuestClick listener) {
        this.questList = quests;
        this.listener = listener;
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quest_completed, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalQuest lq = questList.get(position);

        //Imposta i testi base
        holder.questName.setText(lq.getName());
        holder.questType.setText(lq.getType());
        
        //Calcola il punteggio totale
        int timesCompleted = lq.getTimesCompleted();
        int totalPoints = lq.getRewardPoints() * timesCompleted;

        //Imposta il testo del punteggio totale
        String totalPointsText = holder.itemView.getContext().getString(R.string.quest_reward_points, totalPoints);
        holder.questRewardPoints.setText(totalPointsText);

        //Gestione dell'immagine
        int imageResId = lq.getQuestImageResourceId(holder.itemView.getContext());
        holder.questImage.setImageResource(imageResId != 0 ? imageResId : R.drawable.ic_launcher_background);

        //Imposta il testo delle volte completate
        String timesCompletedText = holder.itemView.getContext().getString(R.string.quest_times_completed, timesCompleted);
        holder.questTimesCompleted.setText(timesCompletedText);

        //Listener per il click sulla card
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(lq.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return questList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView questImage;
        TextView questName, questType, questTimesCompleted, questRewardPoints;

        ViewHolder(View v) {
            super(v);
            questImage = v.findViewById(R.id.questImage);
            questName = v.findViewById(R.id.quest_name);
            questType = v.findViewById(R.id.txtQuestType);
            questRewardPoints = v.findViewById(R.id.txtQuestRewardPoints);
            questTimesCompleted = v.findViewById(R.id.txtTimeCompleted);
        }
    }
}
