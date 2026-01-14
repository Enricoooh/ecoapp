package com.example.ecoapp.quest_files;

import androidx.recyclerview.widget.DiffUtil;
import com.ecoapp.android.auth.models.Quest;
import java.util.List;

public class QuestDiffCallback extends DiffUtil.Callback {

    private final List<Quest> oldList;
    private final List<Quest> newList;

    public QuestDiffCallback(List<Quest> oldList, List<Quest> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Confronta gli ID: se gli ID sono uguali, l'elemento è lo stesso
        return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Confronta il contenuto: se i dati interni sono cambiati (es. punti o descrizione)
        Quest oldQuest = oldList.get(oldItemPosition);
        Quest newQuest = newList.get(newItemPosition);
        return oldQuest.getName().equals(newQuest.getName()) &&
                oldQuest.getRewardPoints() == newQuest.getRewardPoints();
    }
}