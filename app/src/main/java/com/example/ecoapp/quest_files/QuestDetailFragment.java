package com.example.ecoapp.quest_files;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ecoapp.R;
import com.ecoapp.android.auth.models.Quest;
import java.util.ArrayList;
import java.util.List;

public class QuestDetailFragment extends Fragment {

    public QuestDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_quest_detail, container, false);

        Bundle args = getArguments();
        if (args != null) {
            int questId = args.getInt("questId", 0);

            // Cerchiamo l'oggetto Quest reale usando l'ID ricevuto
            Quest quest = findQuestById(questId);

            if (quest != null) {
                populateUI(v, quest);
            }
        }
        return v;
    }

    private void populateUI(View v, Quest quest) {
        // Riferimenti agli elementi del layout
        ImageView img = v.findViewById(R.id.imgDetailQuest);
        TextView txtName = v.findViewById(R.id.txtDetailName);
        TextView txtDesc = v.findViewById(R.id.txtDetailDescription);
        TextView txtType = v.findViewById(R.id.txtDetailType);
        TextView txtReward = v.findViewById(R.id.txtDetailPoints); // Questo è il campo Reward
        ProgressBar progressBar = v.findViewById(R.id.progressDetail);
        LinearLayout euContainer = v.findViewById(R.id.containerEuGoals);

        // Impostazione dati dall'oggetto Quest
        img.setImageResource(quest.getImageResId());
        txtName.setText(quest.getName());
        txtDesc.setText(quest.getDescription());
        txtType.setText(quest.getType());

        // Utilizzo della stringa reward_label (Risolve il Warning)
        // Sostituisce %1$d con il valore di getReward_points()
        txtReward.setText(getString(R.string.reward_label, quest.getReward_points()));

        // Progresso
        progressBar.setMax(quest.getMaxProgress());
        progressBar.setProgress(quest.getActualProgress());

        // Icone EU Goals (Aggiunta dinamica)
        if (quest.getImages_eu_goals() != null) {
            euContainer.removeAllViews(); // Pulisce icone precedenti
            for (int resId : quest.getImages_eu_goals()) {
                ImageView iv = new ImageView(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(120, 120);
                lp.setMargins(0, 0, 16, 0);
                iv.setLayoutParams(lp);
                iv.setImageResource(resId);
                euContainer.addView(iv);
            }
        }
    }

    // Simulazione database: deve contenere gli stessi dati del QuestFragment
    private Quest findQuestById(int id) {
        List<Quest> questList = new ArrayList<>();
        questList.add(new Quest(0, "Quest A", "Alimentation", 10, 100, R.drawable.ic_launcher_background, "Mangia sano", new int[]{}, 230));
        questList.add(new Quest(1, "Quest B", "Mobility", 15, 20, R.drawable.ic_launcher_background, "Usa la bici", new int[]{}, 75));
        questList.add(new Quest(2, "Quest C", "Energy", 3, 7, R.drawable.ic_launcher_background, "Spegni le luci", new int[]{}, 15));

        for (Quest q : questList) {
            if (q.getId() == id) return q;
        }
        return null;
    }
}