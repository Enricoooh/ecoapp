package com.example.ecoapp.quest_files;

import android.view.*;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;import com.example.ecoapp.R;
import com.ecoapp.android.auth.models.LocalQuest;
import com.google.android.material.progressindicator.LinearProgressIndicator; // AGGIUNTO

import java.util.List;

public class OngoingQuestAdapter2 extends RecyclerView.Adapter<OngoingQuestAdapter2.ViewHolder> {

    private final List<LocalQuest> questList;
    private final OnQuestClick listener;

    public interface OnQuestClick {
        void onClick(int questId);
    }

    // Costruttore aggiornato per ricevere anche i progressi
    public OngoingQuestAdapter2(List<LocalQuest> quests, OnQuestClick listener) {
        this.questList = quests;
        this.listener = listener;
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quest_ongoing2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalQuest lq = questList.get(position);

        //Imposta i testi base
        holder.questName.setText(lq.getName());
        holder.questType.setText(lq.getType());

        //Imposta i punti (quelli che volevi a destra nel RelativeLayout)
        String rewardText = holder.itemView.getContext().getString(R.string.quest_reward_points, lq.getRewardPoints());
        holder.questRewardPoints.setText(rewardText);

        //Variabili per impostare il progresso
        int maxProgress = lq.getMaxProgress();
        int actualProgress = lq.getActualProgress();

        //Gestione Barra di Progresso
        holder.progressBar.setMax(maxProgress);
        holder.progressBar.setProgress(actualProgress);

        //Gestione etichetta della barra di progresso (es. 2/10)
        String progressText = holder.itemView.getContext().getString(R.string.quest_current_progress, actualProgress, maxProgress);
        holder.questProgress.setText(progressText);

        //Gestione dell'immagine
        int imageResId = lq.getQuestImageResourceId(holder.itemView.getContext());
        holder.questImage.setImageResource(imageResId != 0 ? imageResId : R.drawable.ic_launcher_background);

        // Listener per il click sulla card
        //holder.itemView.setOnClickListener(v -> listener.onClick(lq.getId()));
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
        TextView questName, questType, questRewardPoints, questProgress;
        LinearProgressIndicator progressBar; // AGGIUNTO

        ViewHolder(View v) {
            super(v);
            questImage = v.findViewById(R.id.questImage); // Verifica ID nell'XML
            questName = v.findViewById(R.id.txtQuestName);
            questType = v.findViewById(R.id.txtQuestType);
            questRewardPoints = v.findViewById(R.id.txtQuestRewardPoints); // ID del RelativeLayout creato prima
            questProgress = v.findViewById(R.id.txtProgress);
            progressBar = v.findViewById(R.id.progressBarItemQuest);
        }
    }
}