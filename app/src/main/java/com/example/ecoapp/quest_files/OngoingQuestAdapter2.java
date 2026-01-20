package com.example.ecoapp.quest_files;

import android.view.*;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;import com.example.ecoapp.R;
import com.ecoapp.android.auth.models.Quest;
import com.ecoapp.android.auth.models.UserQuest; // AGGIUNTO
import com.google.android.material.progressindicator.LinearProgressIndicator; // AGGIUNTO

import java.util.List;

public class OngoingQuestAdapter2 extends RecyclerView.Adapter<OngoingQuestAdapter2.ViewHolder> {

    private final List<Quest> questList;
    private final List<UserQuest> userProgressList; // AGGIUNTO: per sapere a che punto è l'utente
    private final OnQuestClick listener;

    public interface OnQuestClick {
        void onClick(int questId);
    }

    // Costruttore aggiornato per ricevere anche i progressi
    public OngoingQuestAdapter2(List<Quest> quests, List<UserQuest> progress, OnQuestClick listener) {
        this.questList = quests;
        this.userProgressList = progress;
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
        Quest q = questList.get(position);

        // 1. Trova il progresso corrispondente per questa missione
        UserQuest progress = null;
        for (UserQuest uq : userProgressList) {
            if (uq.getQuestId() == q.getId()) {
                progress = uq;
                break;
            }
        }

        // 2. Imposta i testi base
        holder.questName.setText(q.getName());
        holder.questType.setText(q.getType());

        // Imposta i punti (quelli che volevi a destra nel RelativeLayout)
        String rewardText = holder.itemView.getContext().getString(R.string.quest_reward_points, q.getRewardPoints());
        holder.questRewardPoints.setText(rewardText);

        // 3. Gestione Barra di Progresso e Etichetta (es. 2/10)
        if (progress != null && holder.progressBar != null) {
            int current = progress.getActualProgress();
            int max = q.getMaxProgress();

            holder.progressBar.setMax(max);
            holder.progressBar.setProgress(current);

            //Usa getString con i segnaposti per il progresso
            String progressText = holder.itemView.getContext().getString(R.string.quest_current_progress, current, max);
            holder.questProgress.setText(progressText);
        }

        // 4. Immagine
        int imageResId = q.getQuestImageResourceId(holder.itemView.getContext());
        holder.questImage.setImageResource(imageResId != 0 ? imageResId : R.drawable.ic_launcher_background);

        holder.itemView.setOnClickListener(v -> listener.onClick(q.getId()));
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