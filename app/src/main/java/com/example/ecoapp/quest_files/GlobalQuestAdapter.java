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

public class GlobalQuestAdapter extends RecyclerView.Adapter<GlobalQuestAdapter.ViewHolder> {

    private final List<Quest> questList;
    private final OnQuestClick listener;

    public interface OnQuestClick {
        void onClick(int questId);
    }

    public GlobalQuestAdapter(List<Quest> quests, OnQuestClick listener) {
        this.questList = quests;
        this.listener = listener;
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quest_global, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Quest q = questList.get(position);

        //Imposta il Nome
        holder.questName.setText(q.getName());

        //Imposta il Tipo (es. Alimentation)
        holder.questType.setText(q.getType());

        //Imposta i Punti (es. 50 pts)
        holder.questRewardPoints.setText(holder.itemView.getContext().
                getString(R.string.quest_reward_points, q.getRewardPoints()));

        //Gestione Immagine: usa il metodo creato in Quest.java
        int imageResId = q.getQuestImageResourceId(holder.itemView.getContext());
        holder.questImage.setImageResource(imageResId != 0 ? imageResId : R.drawable.ic_launcher_background);

        //Gestione Click per navigare ai dettagli
        //holder.itemView.setOnClickListener(v -> listener.onClick(q.getId()));
        // Listener per il click sulla card
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(q.getId());
            }
        });
    }


    @Override
    public int getItemCount() {
        return questList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView questImage;
        TextView questName;
        TextView questType;
        TextView questRewardPoints;

        ViewHolder(View v) {
            super(v);
            questImage = v.findViewById(R.id.questImage);
            questName = v.findViewById(R.id.txtQuestName);
            questType = v.findViewById(R.id.txtQuestType);
            questRewardPoints = v.findViewById(R.id.txtQuestRewardPoints);
        }
    }
}