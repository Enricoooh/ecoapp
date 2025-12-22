package com.example.ecoapp.quest_files;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.widget.TextView;

import com.example.ecoapp.R;


public class QuestDetailFragment extends Fragment {

    public QuestDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_quest_detail, container, false);

        Bundle args = getArguments();
        int questId = 0; // valore di default
        if (args != null) {
            questId = args.getInt("questId", 0); // 0 come default se la chiave non esiste
        }
        ((TextView)v.findViewById(R.id.txtDetail)).setText(getString(R.string.quest_detail, questId));


        return v;
    }
}