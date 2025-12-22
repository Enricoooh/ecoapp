package com.example.ecoapp.quest_files;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

//Liste + Quest
import java.util.List;
import java.util.ArrayList;
import com.ecoapp.android.auth.models.Quest;

//RecyclerView e Adapter
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.ecoapp.R;

public class QuestFragment extends Fragment {

    public QuestFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_quest, container, false);

        //Lista
        List<Quest> questList = new ArrayList<>();
        questList.add(new Quest(0, "Quest A", "Alimentation", 10, 100, R.drawable.ic_launcher_background, "Descrizione", new int[]{}, 230));
        questList.add(new Quest(1, "Quest B", "Mobility", 15, 20, R.drawable.ic_launcher_background, "Descrizione", new int[]{}, 75));
        questList.add(new Quest(2, "Quest C", "Energy", 3, 7, R.drawable.ic_launcher_background, "Descrizione", new int[]{}, 15));



        //RecyclerView e Adapter
        RecyclerView rv = v.findViewById(R.id.recyclerQuests);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new QuestAdapter(questList, id -> {
            Bundle b = new Bundle();
            b.putInt("questId", id);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_questFragment_to_questDetailFragment, b);
        }));

        return v;
    }
}